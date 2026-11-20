#!/usr/bin/env python3
"""
Generate SwiftDrop Database Architecture Diagram
Creates a comprehensive PNG image showing all 8 databases, their tables,
and the microservice-to-database connections.
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

# Colors
NAVY = '#10243A'
TEAL = '#0D9488'
LIGHT_TEAL = '#14B8A6'
SLATE = '#F8FAFC'
WHITE = '#FFFFFF'
LIGHT_GRAY = '#E2E8F0'
DARK_GRAY = '#334155'
ACCENT_BLUE = '#3B82F6'
ACCENT_PURPLE = '#8B5CF6'
ACCENT_ORANGE = '#F59E0B'
ACCENT_RED = '#EF4444'
ACCENT_GREEN = '#22C55E'
ACCENT_PINK = '#EC4899'
ACCENT_CYAN = '#06B6D4'
ACCENT_INDIgo = '#6366F1'

def draw_service_box(ax, x, y, w, h, service_name, port, color, db_name):
    """Draw a microservice box with its database name."""
    # Service box
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08",
                         facecolor=color, edgecolor=NAVY, linewidth=2, alpha=0.9)
    ax.add_patch(box)
    
    # Service name
    ax.text(x + w/2, y + h*0.65, service_name,
           ha='center', va='center', fontsize=11, fontweight='bold', color=WHITE)
    
    # Port
    ax.text(x + w/2, y + h*0.4, f'Port {port}',
           ha='center', va='center', fontsize=8, color=WHITE, alpha=0.8)
    
    # DB name
    ax.text(x + w/2, y + h*0.15, f'DB: {db_name}',
           ha='center', va='center', fontsize=7, color=WHITE,
           style='italic', alpha=0.9)

def draw_db_table(ax, x, y, w, h, db_name, tables, color):
    """Draw a database cylinder with table names."""
    # Database body
    body = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1",
                         facecolor=SLATE, edgecolor=color, linewidth=2.5)
    ax.add_patch(body)
    
    # Top ellipse (cylinder effect)
    from matplotlib.patches import Ellipse
    ellipse = Ellipse((x + w/2, y + h), w*0.9, h*0.12,
                     facecolor=color, alpha=0.3, edgecolor=color, linewidth=1.5)
    ax.add_patch(ellipse)
    
    # DB name header
    header = FancyBboxPatch((x, y + h - 0.35), w, 0.35, boxstyle="round,pad=0.02",
                           facecolor=color, edgecolor='none', alpha=0.9)
    ax.add_patch(header)
    ax.text(x + w/2, y + h - 0.17, db_name,
           ha='center', va='center', fontsize=8, fontweight='bold', color=WHITE)
    
    # Table names
    table_y = y + h - 0.55
    for i, table in enumerate(tables):
        ax.text(x + 0.15, table_y - i*0.28, f'▸ {table}',
               ha='left', va='top', fontsize=6.5, color=DARK_GRAY,
               family='monospace')

def draw_connection(ax, x1, y1, x2, y2, color='gray', style='-', alpha=0.5, width=1.5):
    """Draw a connection line between two points."""
    arrow = FancyArrowPatch((x1, y1), (x2, y2),
                           arrowstyle='->', mutation_scale=12,
                           linewidth=width, color=color, alpha=alpha,
                           linestyle=style, connectionstyle='arc3,rad=0')
    ax.add_patch(arrow)

def main():
    # Create figure
    fig, ax = plt.subplots(1, 1, figsize=(28, 40))
    fig.patch.set_facecolor(WHITE)
    ax.set_xlim(0, 28)
    ax.set_ylim(0, 40)
    ax.axis('off')
    
    # ============================================================
    # TITLE
    # ============================================================
    ax.text(14, 39, 'SwiftDrop — Database Architecture',
           ha='center', va='center', fontsize=26, fontweight='bold', color=NAVY)
    ax.text(14, 37.8, 'Database-per-Service Pattern with PostgreSQL 16',
           ha='center', va='center', fontsize=13, color=DARK_GRAY, style='italic')
    
    # ============================================================
    # POSTGRESQL SERVER (center)
    # ============================================================
    pg_x, pg_y, pg_w, pg_h = 6, 33, 16, 2.5
    pg_box = FancyBboxPatch((pg_x, pg_y), pg_w, pg_h, boxstyle="round,pad=0.2",
                           facecolor=ACCENT_CYAN, edgecolor=NAVY, linewidth=3)
    ax.add_patch(pg_box)
    ax.text(pg_x + pg_w/2, pg_y + pg_h/2 + 0.15, 'PostgreSQL 16  (swiftdrop-postgres)',
           ha='center', va='center', fontsize=15, fontweight='bold', color=WHITE)
    ax.text(pg_x + pg_w/2, pg_y + pg_h/2 - 0.3, 'Docker Container  •  Port 5433:5432  •  Volume: postgres_data',
           ha='center', va='center', fontsize=9, color=WHITE, alpha=0.9)
    
    # ============================================================
    # 8 DATABASES (2 rows of 4)
    # ============================================================
    db_configs = [
        # Row 1
        {'name': 'swiftdrop_identity', 'x': 0.3, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_BLUE, 'tables': ['users', 'roles', 'user_roles', 'oauth_identities',
                                          'merchant_approval_records', 'refresh_sessions',
                                          'password_reset_tokens', 'account_settings']},
        {'name': 'swiftdrop_parcel', 'x': 3.7, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_GREEN, 'tables': ['parcels', 'parcel_status_history',
                                           'parcel_estimates']},
        {'name': 'swiftdrop_merchant', 'x': 7.1, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_PURPLE, 'tables': ['merchant_profiles', 'merchant_api_keys',
                                            'merchant_orders', 'merchant_webhooks',
                                            'merchant_processed_events',
                                            'merchant_webhook_deliveries']},
        {'name': 'swiftdrop_dispatch', 'x': 10.5, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_ORANGE, 'tables': ['couriers', 'dispatch_assignments',
                                            'proof_of_delivery']},
        # Row 2
        {'name': 'swiftdrop_pricing', 'x': 13.9, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_RED, 'tables': ['delivery_zones', 'pricing_rules',
                                         'merchant_tier_discounts', 'promo_codes',
                                         'promo_redemptions', 'fuel_surcharge_index',
                                         'price_quotes']},
        {'name': 'swiftdrop_tracking', 'x': 17.3, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_PINK, 'tables': ['tracking_parcels', 'tracking_events']},
        {'name': 'swiftdrop_notification', 'x': 20.7, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_INDIgo, 'tables': ['notification_logs', 'email_preferences']},
        {'name': 'swiftdrop_reports', 'x': 24.1, 'y': 24, 'w': 3.3, 'h': 5,
         'color': ACCENT_CYAN, 'tables': ['daily_delivery_stats', 'processed_events']},
    ]
    
    # Draw databases
    for db in db_configs:
        draw_db_table(ax, db['x'], db['y'], db['w'], db['h'],
                     db['name'], db['tables'], db['color'])
    
    # Connections from PostgreSQL to databases
    for db in db_configs:
        draw_connection(ax, pg_x + pg_w/2, pg_y,
                       db['x'] + db['w']/2, db['y'] + db['h'],
                       color=db['color'], alpha=0.4, style='--')
    
    # ============================================================
    # 8 MICROSERVICES (2 rows of 4)
    # ============================================================
    service_configs = [
        # Row 1
        {'name': 'Identity\nService', 'port': '8081', 'x': 0.3, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_BLUE, 'db_name': 'swiftdrop_identity'},
        {'name': 'Parcel\nService', 'port': '8082', 'x': 3.7, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_GREEN, 'db_name': 'swiftdrop_parcel'},
        {'name': 'Merchant\nService', 'port': '8083', 'x': 7.1, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_PURPLE, 'db_name': 'swiftdrop_merchant'},
        {'name': 'Dispatch\nService', 'port': '8084', 'x': 10.5, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_ORANGE, 'db_name': 'swiftdrop_dispatch'},
        # Row 2
        {'name': 'Pricing\nService', 'port': '8085', 'x': 13.9, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_RED, 'db_name': 'swiftdrop_pricing'},
        {'name': 'Tracking\nService', 'port': '8086', 'x': 17.3, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_PINK, 'db_name': 'swiftdrop_tracking'},
        {'name': 'Notification\nService', 'port': '8087', 'x': 20.7, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_INDIgo, 'db_name': 'swiftdrop_notification'},
        {'name': 'AI Assistant\nService', 'port': '8089', 'x': 24.1, 'y': 17.5, 'w': 3.3, 'h': 1.6,
         'color': ACCENT_CYAN, 'db_name': 'swiftdrop_reports'},
    ]
    
    # Draw services
    for svc in service_configs:
        draw_service_box(ax, svc['x'], svc['y'], svc['w'], svc['h'],
                        svc['name'], svc['port'], svc['color'], svc['db_name'])
    
    # Connections from services to their databases
    for i, svc in enumerate(service_configs):
        db = db_configs[i]
        draw_connection(ax, svc['x'] + svc['w']/2, svc['y'],
                       db['x'] + db['w']/2, db['y'] + db['h'],
                       color=svc['color'], alpha=0.6)
    
    # ============================================================
    # RABBITMQ EVENT BUS
    # ============================================================
    rmq_x, rmq_y, rmq_w, rmq_h = 7, 14.5, 14, 1.5
    rmq_box = FancyBboxPatch((rmq_x, rmq_y), rmq_w, rmq_h, boxstyle="round,pad=0.15",
                            facecolor=ACCENT_ORANGE, edgecolor=NAVY, linewidth=2.5)
    ax.add_patch(rmq_box)
    ax.text(rmq_x + rmq_w/2, rmq_y + rmq_h/2 + 0.1, 'RabbitMQ 3  —  Event Bus',
           ha='center', va='center', fontsize=12, fontweight='bold', color=WHITE)
    ax.text(rmq_x + rmq_w/2, rmq_y + rmq_h/2 - 0.2,
           'parcel.created  •  parcel.status_updated  •  courier.assigned  •  notification.dispatch',
           ha='center', va='center', fontsize=7, color=WHITE, alpha=0.85)
    
    # Connections from services to RabbitMQ
    for svc in service_configs:
        if svc['name'] not in ['AI Assistant\nService']:
            draw_connection(ax, svc['x'] + svc['w']/2, svc['y'] + svc['h'],
                           rmq_x + rmq_w/2, rmq_y,
                           color=ACCENT_ORANGE, alpha=0.3, style=':')
    
    # ============================================================
    # API GATEWAY
    # ============================================================
    gw_x, gw_y, gw_w, gw_h = 8, 11.5, 12, 1.8
    gw_box = FancyBboxPatch((gw_x, gw_y), gw_w, gw_h, boxstyle="round,pad=0.15",
                           facecolor=NAVY, edgecolor=TEAL, linewidth=3)
    ax.add_patch(gw_box)
    ax.text(gw_x + gw_w/2, gw_y + gw_h/2 + 0.15, 'Spring Cloud Gateway',
           ha='center', va='center', fontsize=13, fontweight='bold', color=WHITE)
    ax.text(gw_x + gw_w/2, gw_y + gw_h/2 - 0.2, 'Port 8080  •  Edge Routing  •  CORS  •  Correlation IDs  •  Security Headers',
           ha='center', va='center', fontsize=8, color=WHITE, alpha=0.85)
    
    # Connections from gateway to services
    for svc in service_configs:
        draw_connection(ax, gw_x + gw_w/2, gw_y,
                       svc['x'] + svc['w']/2, svc['y'] + svc['h'],
                       color=NAVY, alpha=0.3, style='--')
    
    # ============================================================
    # FRONTEND
    # ============================================================
    fe_x, fe_y, fe_w, fe_h = 8, 9, 12, 1.8
    fe_box = FancyBboxPatch((fe_x, fe_y), fe_w, fe_h, boxstyle="round,pad=0.15",
                           facecolor=TEAL, edgecolor=NAVY, linewidth=2.5)
    ax.add_patch(fe_box)
    ax.text(fe_x + fe_w/2, fe_y + fe_h/2 + 0.15, 'React 18 Frontend  (TypeScript / Vite / Tailwind CSS)',
           ha='center', va='center', fontsize=12, fontweight='bold', color=WHITE)
    ax.text(fe_x + fe_w/2, fe_y + fe_h/2 - 0.2, 'Port 3000  •  Nginx  •  React Router  •  TanStack Query  •  Axios',
           ha='center', va='center', fontsize=8, color=WHITE, alpha=0.85)
    
    # Connection from frontend to gateway
    draw_connection(ax, fe_x + fe_w/2, fe_y,
                   gw_x + gw_w/2, gw_y + gw_h,
                   color=TEAL, alpha=0.7, width=2.5)
    
    # ============================================================
    # TECH STACK LEGEND
    # ============================================================
    legend_y = 5
    ax.text(14, legend_y + 2, 'Technology Stack',
           ha='center', va='center', fontsize=13, fontweight='bold', color=NAVY)
    
    tech_items = [
        ('Java 21 LTS', ACCENT_ORANGE),
        ('Spring Boot 3.4.x', ACCENT_GREEN),
        ('Spring Data JPA + Hibernate 6.x', ACCENT_BLUE),
        ('Flyway Migrations', ACCENT_PURPLE),
        ('HikariCP Connection Pool', ACCENT_CYAN),
        ('PostgreSQL JDBC Driver', ACCENT_INDIgo),
        ('H2 (Testing)', ACCENT_PINK),
        ('RabbitMQ 3 (Event Bus)', ACCENT_ORANGE),
    ]
    
    legend_x = 1.5
    for i, (tech, color) in enumerate(tech_items):
        row = i // 4
        col = i % 4
        x = legend_x + col * 6.5
        y = legend_y - row * 0.5
        dot = mpatches.Circle((x, y), 0.12, facecolor=color, edgecolor='none')
        ax.add_patch(dot)
        ax.text(x + 0.3, y, tech, ha='left', va='center', fontsize=9, color=DARK_GRAY)
    
    # ============================================================
    # KEY PATTERNS
    # ============================================================
    patterns_y = 2
    ax.text(14, patterns_y + 1.2, 'Key Database Design Patterns',
           ha='center', va='center', fontsize=12, fontweight='bold', color=NAVY)
    
    patterns = [
        'UUID Primary Keys', 'TIMESTAMP WITH TIME ZONE',
        'Foreign Key Constraints (CASCADE/RESTRICT)', 'B-tree Indexes',
        'Audit Trail Tables', 'BCrypt Password Hashing',
        'SHA-256 Token Hashing', 'Flyway Schema Versioning',
    ]
    
    for i, pattern in enumerate(patterns):
        row = i // 4
        col = i % 4
        x = 1.5 + col * 6.5
        y = patterns_y - row * 0.45
        ax.text(x, y, f'✓ {pattern}', ha='left', va='center',
               fontsize=8, color=TEAL, fontweight='bold')
    
    # ============================================================
    # FOOTER
    # ============================================================
    ax.text(14, 0.3, 'SwiftDrop Courier Management System  •  Enterprise Application Development  •  November 2026',
           ha='center', va='center', fontsize=9, color=LIGHT_GRAY, style='italic')
    
    # Save
    plt.tight_layout()
    plt.savefig('docs/database-architecture.png', dpi=150, bbox_inches='tight',
               facecolor=fig.get_facecolor(), edgecolor='none', pad_inches=0.3)
    print("Database architecture diagram saved to docs/database-architecture.png")

if __name__ == '__main__':
    main()
