import { describe, expect, it } from 'vitest';
import { demoWorkspace, estimatePrice, parseWorkspace, shipmentCsv, trackingSteps } from '../pages/customer/customerData';

const sample = demoWorkspace({ name: 'Test Customer', email: 'test@example.com', phone: '' }, new Date('2026-11-03T10:00:00Z'));

describe('customer demo data', () => {
  it('restores valid shipments and rejects corrupt or invalid stored data', () => {
    expect(parseWorkspace(JSON.stringify(sample), sample)).toEqual(sample);
    expect(parseWorkspace('{bad json', sample)).toBe(sample);
    expect(parseWorkspace(JSON.stringify({ ...sample, shipments: [{ id: 'bad' }] }), sample)).toBe(sample);
  });
  it('does not show pickup or delivery as completed for a newly created shipment', () => {
    expect(trackingSteps('Awaiting pickup').map((s) => s.complete)).toEqual([true, false, false, false]);
    expect(trackingSteps('In transit').map((s) => s.complete)).toEqual([true, true, true, false]);
    expect(trackingSteps('Delivered').every((s) => s.complete)).toBe(true);
    expect(trackingSteps('Cancelled').some((s) => s.complete)).toBe(false);
  });
  it('rounds started kilograms and applies the express surcharge', () => {
    expect(estimatePrice(1, 'Standard')).toBe(600);
    expect(estimatePrice(1.1, 'Standard')).toBe(750);
    expect(estimatePrice(1.1, 'Express')).toBe(1100);
  });
  it('quotes CSV fields and neutralizes spreadsheet formulas', () => {
    const csv = shipmentCsv([{ ...sample.shipments[0], recipient: '=1+1', destination: '12 "Main", Road' }]);
    expect(csv).toContain('"\'=1+1"');
    expect(csv).toContain('"12 ""Main"", Road"');
    expect(csv.split('\r\n')).toHaveLength(2);
  });
});
