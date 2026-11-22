# CloudPulse — AWS Production Deployment Architecture & Blueprint

> **Notice**: In accordance with project budget constraints (₹0), this document serves as the comprehensive architectural specification, infrastructure blueprint, and deployment playbook without provisioning active paid cloud resources.

---

## 1. High-Level Architecture Overview

CloudPulse is designed as a cloud-native, multi-tier reliability monitoring system engineered for high availability (HA), zero-downtime rolling upgrades, strict tenant isolation, and regulatory audit compliance.

```
                              [ Internet Users / API Clients ]
                                             │
                                             ▼
                             [ Route 53 (DNS & Health Checks) ]
                                             │
                                             ▼
                          [ AWS WAF (Web Application Firewall) ]
                           - Rate Limiting (100 req/min)
                           - SQLi & XSS Shield
                           - SSRF / RFC1918 Block
                                             │
                                             ▼
                      [ Public Application Load Balancer (ALB) ]
                         TLS 1.3 Termination (ACM Certificate)
                                      /              \
                        /api/* (Path Rule)      /* (Default)
                                  /                      \
                                 ▼                        ▼
              [ ECS Fargate: Backend Cluster ]   [ ECS Fargate: Frontend Nginx ]
              - Private App Subnet (AZ-A & B)     - Private App Subnet (AZ-A & B)
              - Spring Boot 3.3.5 / Java 21       - React 18 / Vite / Static Nginx
              - Horizontal Pod Autoscaler (HPA)   - Auto-scaled on CPU / Memory
                       │              │
        ┌──────────────┴──────┐       └──────────────┬──────────────────┐
        ▼                     ▼                      ▼                  ▼
[ Amazon RDS Postgres 16 ] [ Amazon S3 ]     [ Amazon SES ]   [ CloudWatch & X-Ray ]
- Multi-AZ Replica         - PDF Reports     - TLS SMTP       - Distributed Tracing
- KMS Encrypted Storage    - Versioned       - DMARC/DKIM     - Alarms & Metrics
- Automated Daily Snapshots - SSE-S3 Encrypted
```

---

## 2. Network Topology & VPC Segmentation

The AWS deployment utilizes an isolated Amazon Virtual Private Cloud (VPC) spanning two Availability Zones in the primary region (e.g., `ap-south-1` Mumbai or `us-east-1` N. Virginia):

### 2.1 Subnet Allocation Matrix
| Subnet Tier | Availability Zone | CIDR Block | Route Target | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Public A** | `ap-south-1a` | `10.0.1.0/24` | Internet Gateway (IGW) | External ALB Ingress & NAT Gateway A |
| **Public B** | `ap-south-1b` | `10.0.2.0/24` | Internet Gateway (IGW) | External ALB Ingress & NAT Gateway B |
| **Private App A** | `ap-south-1a` | `10.0.10.0/24` | NAT Gateway A | ECS Backend & Frontend Containers |
| **Private App B** | `ap-south-1b` | `10.0.11.0/24` | NAT Gateway B | ECS Backend & Frontend Containers |
| **Private Data A**| `ap-south-1a` | `10.0.20.0/24` | Isolated (No IGW/NAT) | Amazon RDS PostgreSQL Primary |
| **Private Data B**| `ap-south-1b` | `10.0.21.0/24` | Isolated (No IGW/NAT) | Amazon RDS PostgreSQL Standby Replica |

### 2.2 Security Group Rules (Least Privilege)
1. **ALB Security Group (`sg-cloudpulse-alb`)**:
   - Inbound: `TCP 80` (HTTP redirected to HTTPS) from `0.0.0.0/0`
   - Inbound: `TCP 443` (HTTPS TLS 1.3) from `0.0.0.0/0`
   - Outbound: `TCP 8081` to `sg-cloudpulse-ecs-backend`
   - Outbound: `TCP 80` to `sg-cloudpulse-ecs-frontend`
2. **Backend ECS Security Group (`sg-cloudpulse-ecs-backend`)**:
   - Inbound: `TCP 8081` strictly from `sg-cloudpulse-alb`
   - Outbound: `TCP 5432` to `sg-cloudpulse-rds`
   - Outbound: `TCP 443` to AWS Endpoints / Internet (monitoring checks)
   - Outbound: `TCP 587` to Amazon SES SMTP endpoint
3. **Frontend ECS Security Group (`sg-cloudpulse-ecs-frontend`)**:
   - Inbound: `TCP 80` strictly from `sg-cloudpulse-alb`
   - Outbound: None
4. **RDS Security Group (`sg-cloudpulse-rds`)**:
   - Inbound: `TCP 5432` strictly from `sg-cloudpulse-ecs-backend`
   - Outbound: None (fully air-gapped data tier)

---

## 3. Core AWS Services Specification

### 3.1 Container Compute (Amazon ECS on AWS Fargate)
- **Backend Service**:
  - Task Definition: 1 vCPU, 2 GB RAM (`FARGATE`)
  - Auto-Scaling Target: Min 2 tasks, Max 10 tasks
  - Scaling Policy: Target Tracking on Average CPU Utilization (70%) and ALB Request Count per Target (1000 req/min).
  - Health Check Grace Period: 60 seconds
- **Frontend Service**:
  - Task Definition: 0.5 vCPU, 1 GB RAM (`FARGATE`)
  - Min 2 tasks, Max 4 tasks running lightweight Nginx reverse proxy.

### 3.2 Relational Database (Amazon RDS PostgreSQL 16)
- **Engine**: PostgreSQL 16.3
- **Instance Type**: `db.t4g.medium` (Production) or `db.t4g.micro` (AWS Free Tier eligible)
- **Storage**: 50 GB gp3 with storage autoscaling up to 500 GB
- **High Availability**: Multi-AZ Deployment with automated synchronous replication
- **Backup & Recovery**: 30-day automated backup retention, Point-in-Time Recovery (PITR) enabled
- **Encryption**: AWS KMS Customer Managed Key (`aws/rds`) for data-at-rest encryption.

### 3.3 Storage (Amazon S3)
- **Bucket**: `cloudpulse-reports-production-<account-id>`
- **Encryption**: SSE-S3 (AES-256) enabled by default
- **Bucket Policy**: Enforce TLS (`aws:SecureTransport: true`), Block all public access
- **Lifecycle Policy**:
  - Transition objects to S3 Standard-Infrequent Access (Standard-IA) after 90 days.
  - Expire / Archive audit logs after 365 days (meeting compliance standards).

### 3.4 Email Notification Delivery (Amazon SES)
- Domain verified with DKIM (Easy DKIM 2048-bit keys), SPF record (`v=spf1 include:amazonses.com ~all`), and DMARC policy (`v=DMARC1; p=quarantine;`).
- Connected via Spring Boot `JavaMailSender` on `email-smtp.ap-south-1.amazonaws.com:587` with STARTTLS.

### 3.5 Secrets Management & Configuration
- **AWS Secrets Manager**:
  - `cloudpulse/production/database`: RDS credentials
  - `cloudpulse/production/jwt`: 256-bit HMAC secret key
  - `cloudpulse/production/ses`: SMTP credentials
- Injected automatically into ECS Task Definitions at container launch via IAM execution role.

---

## 4. Terraform Infrastructure as Code (IaC) Blueprint

```hcl
# main.tf - CloudPulse AWS Infrastructure Blueprint

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "CloudPulse"
      Environment = "Production"
      ManagedBy   = "Terraform"
    }
  }
}

# 1. VPC & Networking
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.0.0"

  name = "cloudpulse-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["${var.aws_region}a", "${var.aws_region}b"]
  public_subnets  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnets = ["10.0.10.0/24", "10.0.11.0/24"]
  database_subnets = ["10.0.20.0/24", "10.0.21.0/24"]

  enable_nat_gateway     = true
  single_nat_gateway     = false
  one_nat_gateway_per_az = true
  enable_dns_hostnames   = true
  enable_dns_support     = true
}

# 2. RDS PostgreSQL Database
resource "aws_db_instance" "postgres" {
  identifier           = "cloudpulse-postgres-prod"
  engine               = "postgres"
  engine_version       = "16.3"
  instance_class       = "db.t4g.medium"
  allocated_storage    = 50
  max_allocated_storage = 250
  storage_type         = "gp3"
  multi_az             = true

  db_name  = "cloudpulse"
  username = "cloudpulse_admin"
  password = var.db_master_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = module.vpc.database_subnet_group_name

  skip_final_snapshot    = false
  final_snapshot_identifier = "cloudpulse-postgres-final-snapshot"
  deletion_protection   = true
  storage_encrypted     = true
}

# 3. ECS Cluster & Fargate Services
resource "aws_ecs_cluster" "cluster" {
  name = "cloudpulse-prod-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# 4. S3 Bucket for PDF Reports
resource "aws_s3_bucket" "reports" {
  bucket = "cloudpulse-reports-${var.environment}"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "reports_enc" {
  bucket = aws_s3_bucket.reports.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "reports_block" {
  bucket = aws_s3_bucket.reports.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

---

## 5. Zero-Downtime Deployment & Migration Pipeline

```
1. Developer pushes code to `main` branch.
2. GitHub Actions runs CI:
   - Backend `mvn clean test` (129 unit & integration tests)
   - Frontend `npm test` (38 unit tests) & `npm run build`
   - Build & push Docker images to Amazon Elastic Container Registry (ECR)
3. Pre-Deployment Task (ECS RunTask):
   - Standalone container runs Flyway database migrations (`V1` through `V5`) against RDS.
   - Backward-compatible schema changes ensure zero downtime for existing running tasks.
4. Rolling Service Deployment (AWS CodeDeploy / ECS):
   - Spawns new task revision alongside existing tasks.
   - ALB health check verifies `/actuator/health` returns `HTTP 200 {"status":"UP"}`.
   - Once healthy, ALB shifts traffic gracefully to new tasks.
   - Old task instances are drained and decommissioned.
```

---

## 6. Disaster Recovery & SLA Specifications

| Objective | Target | Implementation Mechanism |
| :--- | :--- | :--- |
| **RTO (Recovery Time Objective)** | < 15 minutes | ECS Fargate automated container replacement + Multi-AZ RDS automatic failover |
| **RPO (Recovery Point Objective)** | < 5 minutes | PostgreSQL continuous WAL archiving with AWS RDS Point-in-Time Recovery |
| **Availability SLA** | 99.95% | Multi-AZ deployment, redundant NAT gateways, autoscaled tasks across 2 AZs |
| **Audit Compliance** | SOC 2 / ISO 27001 | KMS encryption at rest, TLS 1.3 in transit, immutably logged PDF report generation |
