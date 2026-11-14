INSERT INTO pricing_rules(service_type,base_fare,base_weight_kg,per_kg_rate,per_km_rate,volumetric_divisor,urgency_multiplier,is_active)
VALUES ('STANDARD',250,1,80,0,5000,1,true),('EXPRESS',250,1,80,0,5000,1.5,true),('SAME_DAY',250,1,80,0,5000,2.2,true)
ON CONFLICT (service_type) DO NOTHING;
