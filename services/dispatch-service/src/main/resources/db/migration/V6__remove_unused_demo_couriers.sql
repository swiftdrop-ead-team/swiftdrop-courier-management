-- Remove only the original V2 demonstration couriers that have no delivery history.
-- Keep assigned couriers so existing assignments and proof remain intact.
DELETE FROM couriers c
WHERE c.user_id IN ('courier-uom-001', 'courier-uom-002', 'courier-uom-003', 'courier-uom-004', 'courier-uom-005')
  AND c.current_active_deliveries = 0
  AND NOT EXISTS (SELECT 1 FROM dispatch_assignments a WHERE a.courier_id = c.id);
