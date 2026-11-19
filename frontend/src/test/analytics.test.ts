import { describe, expect, it } from "vitest";
import { bucket, comparison, deliveryHours, permittedParcels, presetRange, previousRange, selectPeriod, summarize, trend, validRange } from "../features/delivery/analytics";
import { emptyBooking, type ParcelRecord } from "../features/delivery/model";
const parcel = (patch: Partial<ParcelRecord> = {}): ParcelRecord => ({ ...emptyBooking(), id: "one", trackingNumber: "SD-ONE", ownerId: "merchant-a", ownerRole: "MERCHANT", createdAt: "2026-11-18T20:00:00Z", status: "DELIVERED", amount: 250, currency: "LKR", history: [], ...patch });
describe("analytics calculations", () => {
  it("limits merchants and couriers to their own records and excludes customer analytics", () => {
    const own = parcel({courierId:"courier-a"}), other = parcel({id:"two",ownerId:"merchant-b",courierId:"courier-b"});
    expect(permittedParcels([own,other],"MERCHANT","merchant-a")).toEqual([own]);
    expect(permittedParcels([own,other],"COURIER","courier-a")).toEqual([own]);
    expect(permittedParcels([own,other],"ADMIN","admin")).toHaveLength(2);
    expect(permittedParcels([own,other],"MERCHANT","")).toEqual([]);
    expect(permittedParcels([own,other],"CUSTOMER","merchant-a")).toEqual([]);
  });
  it("uses Colombo booking dates and assignment dates for courier cohorts", () => {
    const p=parcel({assignedDate:"2026-11-20"});
    expect(selectPeriod([p],{from:"2026-11-19",to:"2026-11-19"})).toHaveLength(1);
    expect(selectPeriod([p],{from:"2026-11-19",to:"2026-11-19"},true)).toHaveLength(0);
    expect(selectPeriod([p],{from:"2026-11-20",to:"2026-11-20"},true)).toHaveLength(1);
    expect(selectPeriod([parcel()],{from:"2026-11-19",to:"2026-11-19"},true)).toHaveLength(0);
  });
  it("separates currencies, excludes cancelled fees and uses completed attempts for success", () => {
    const result=summarize([parcel(),parcel({status:"FAILED",amount:10,currency:"USD"}),parcel({status:"CANCELLED",amount:999}),parcel({status:"CREATED",amount:50})]);
    expect(result.fees).toEqual({LKR:300,USD:10}); expect(result.success).toBe(50); expect(result.pending).toBe(1); expect(result.cancelled).toBe(1); expect(result.hours).toBeNull();
    expect(summarize([]).success).toBeNull(); expect(comparison(5,0)).toBe("No prior baseline");
  });
  it("requires actual ordered pickup and delivery timestamps", () => {
    const p=parcel({history:[{status:"PICKED_UP",timestamp:"2026-11-19T08:00:00",note:""},{status:"DELIVERED",timestamp:"2026-11-19T05:30:00Z",note:""}]});
    expect(deliveryHours(p)).toBe(3);
    expect(deliveryHours(parcel())).toBeNull();
    expect(deliveryHours({...p,status:"FAILED"})).toBeNull();
    expect(deliveryHours({...p,history:[{status:"PICKED_UP",timestamp:"2026-11-20T00:00:00Z",note:""},p.history[1]]})).toBeNull();
  });
  it("compares inclusive equal-length periods across year and leap-day boundaries", () => {
    expect(previousRange({from:"2024-03-01",to:"2024-03-02"})).toEqual({from:"2024-02-28",to:"2024-02-29"});
    expect(presetRange("week","2026-01-02")).toEqual({from:"2025-12-27",to:"2026-01-02"});
    expect(validRange({from:"2026-02-30",to:"2026-03-01"})).toBe(false);
    expect(validRange({from:"",to:"2026-11-19"})).toBe(false);
    expect(validRange({from:"2026-11-20",to:"2026-11-19"})).toBe(false);
  });
  it("fills missing days without inventing events and groups weeks from Monday", () => {
    const result=trend([parcel()],{from:"2026-11-18",to:"2026-11-20"},"day");
    expect(result.map(b=>b.total)).toEqual([0,1,0]);
    expect(bucket("2026-11-22","week")).toBe("2026-11-16");
    expect(bucket("2026-11-23","week")).toBe("2026-11-23");
    expect(trend([parcel()],{from:"2026-11-01",to:"2026-11-30"},"month")[0].fees).toEqual({LKR:250});
  });
});
