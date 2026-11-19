import type { UserRole } from "@/types/auth";
import { day, terminal, type ParcelRecord } from "./model";

export type Period = "day" | "week" | "month" | "year";
export type Range = { from: string; to: string };
const DAY = 86400000;
export function shiftDate(date: string, days: number) {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * DAY).toISOString().slice(0, 10);
}
export function validRange({ from, to }: Range) {
  const valid = (value: string) => /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value;
  return valid(from) && valid(to) && from <= to && (Date.parse(to) - Date.parse(from)) / DAY <= 3660;
}
export function previousRange(range: Range): Range {
  const length = (Date.parse(range.to) - Date.parse(range.from)) / DAY + 1;
  return { from: shiftDate(range.from, -length), to: shiftDate(range.from, -1) };
}
export function presetRange(period: Period, today = day()): Range {
  if (period === "day") return { from: today, to: today };
  if (period === "week") return { from: shiftDate(today, -6), to: today };
  if (period === "month") return { from: today.slice(0, 7) + "-01", to: today };
  return { from: today.slice(0, 4) + "-01-01", to: today };
}
export function permittedParcels(records: ParcelRecord[], role: UserRole, ownerId: string) {
  if (role === "ADMIN") return records;
  if (!ownerId) return [];
  if (role === "MERCHANT" || role === "CUSTOMER") return records.filter(p => p.ownerId === ownerId);
  if (role === "COURIER") return records.filter(p => p.courierId === ownerId);
  return [];
}
export function cohortDate(p: ParcelRecord, courier: boolean) {
  if (courier) return p.assignedDate || "";
  return Number.isFinite(Date.parse(p.createdAt)) ? day(p.createdAt) : "";
}
export function inRange(date: string, range: Range) { return Boolean(date && date >= range.from && date <= range.to); }
export function selectPeriod(records: ParcelRecord[], range: Range, courier = false) {
  return records.filter(p => inRange(cohortDate(p, courier), range));
}
function instant(value: string) {
  return Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}+05:30`);
}
export function deliveryHours(p: ParcelRecord): number | null {
  if (p.status !== "DELIVERED") return null;
  const pickups = p.history.filter(h => h.status === "PICKED_UP").map(h => instant(h.timestamp)).filter(Number.isFinite);
  const deliveries = p.history.filter(h => h.status === "DELIVERED").map(h => instant(h.timestamp)).filter(Number.isFinite);
  if (!pickups.length || !deliveries.length) return null;
  const start = Math.min(...pickups), end = Math.max(...deliveries);
  return end >= start ? (end - start) / 3600000 : null;
}
export function summarize(records: ParcelRecord[]) {
  const delivered = records.filter(p => p.status === "DELIVERED").length;
  const failed = records.filter(p => p.status === "FAILED").length;
  const durations = records.map(deliveryHours).filter((n): n is number => n !== null);
  const fees: Record<string, number> = {};
  records.filter(p => p.status !== "CANCELLED" && Number.isFinite(p.amount)).forEach(p => {
    const currency = p.currency || "Unknown currency";
    fees[currency] = (fees[currency] || 0) + p.amount;
  });
  return { total: records.length, delivered, failed, pending: records.filter(p => !terminal(p.status)).length,
    cancelled: records.filter(p => p.status === "CANCELLED").length,
    success: delivered + failed ? delivered / (delivered + failed) * 100 : null,
    hours: durations.length ? durations.reduce((a, b) => a + b, 0) / durations.length : null,
    timed: durations.length, fees };
}
export function comparison(current: number, previous: number) {
  if (!previous) return current ? "No prior baseline" : "No change";
  const change = (current - previous) / previous * 100;
  return `${change > 0 ? "+" : ""}${change.toFixed(1)}% vs previous period`;
}
export function bucket(date: string, period: Period) {
  if (period === "year") return date.slice(0, 4);
  if (period === "month") return date.slice(0, 7);
  if (period === "week") return shiftDate(date, -(new Date(`${date}T00:00:00Z`).getUTCDay() + 6) % 7);
  return date;
}
export function trend(records: ParcelRecord[], range: Range, period: Period, courier = false) {
  const bins = new Map<string, { label: string; total: number; delivered: number; fees: Record<string, number> }>();
  for (let date = range.from; date <= range.to; date = shiftDate(date, 1)) {
    const key = bucket(date, period);
    if (!bins.has(key)) bins.set(key, { label: key, total: 0, delivered: 0, fees: {} });
  }
  selectPeriod(records, range, courier).forEach(p => {
    const bin = bins.get(bucket(cohortDate(p, courier), period))!;
    bin.total++;
    if (p.status === "DELIVERED") bin.delivered++;
    if (p.status !== "CANCELLED" && Number.isFinite(p.amount)) {
      const currency = p.currency || "Unknown currency";
      bin.fees[currency] = (bin.fees[currency] || 0) + p.amount;
    }
  });
  return [...bins.values()];
}
