import { describe, expect, it } from 'vitest';
import { createAdminPreview, csvDocument, dateTime } from '../pages/admin/adminData';

describe('Admin data helpers', () => {
  it('provides independent sample records when resetting a preview', () => {
    const sample = createAdminPreview();
    sample.merchants[0].approvalStatus = 'APPROVED';
    sample.users[0].fullName = 'Changed';
    expect(createAdminPreview().merchants[0].approvalStatus).toBe('PENDING');
    expect(createAdminPreview().users[0].fullName).toBe('Admin Preview');
  });
  it('quotes CSV fields and neutralizes spreadsheet formulas', () => {
    const result = csvDocument([['Full name', 'Notes'], ['Asha "Silva"', ' =SUM(1,2)']]);
    expect(result).toContain('"Asha ""Silva"""');
    expect(result).toContain('"\' =SUM(1,2)"');
    expect(result.startsWith('\uFEFF')).toBe(true);
  });
  it('handles absent or invalid timestamps', () => {
    expect(dateTime()).toBe('Not available');
    expect(dateTime('invalid')).toBe('Not available');
  });
});
