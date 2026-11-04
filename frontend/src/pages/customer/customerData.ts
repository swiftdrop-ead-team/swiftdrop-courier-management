import { z } from "zod";
export const statuses = [
  "Awaiting pickup",
  "In transit",
  "Delivered",
  "Cancelled",
] as const;
const shipmentSchema = z.object({
  id: z.string(),
  recipient: z.string(),
  phone: z.string(),
  destination: z.string(),
  pickup: z.string(),
  bookedAt: z.string().datetime(),
  status: z.enum(statuses),
  weight: z.number().positive().max(30),
  service: z.enum(["Standard", "Express"]),
  packageType: z.string(),
  amount: z.number().nonnegative(),
  notes: z.string(),
});
export type Shipment = z.infer<typeof shipmentSchema>;
const addressSchema = z.object({
  id: z.string(),
  label: z.string().trim().min(1),
  address: z.string().trim().min(5),
});
export type Address = z.infer<typeof addressSchema>;
const workspaceSchema = z.object({
  version: z.literal(1),
  shipments: z.array(shipmentSchema),
  addresses: z.array(addressSchema),
  profile: z.object({ name: z.string(), email: z.string(), phone: z.string() }),
  preferences: z.object({ updates: z.boolean(), offers: z.boolean() }),
});
export type Workspace = z.infer<typeof workspaceSchema>;
export function demoWorkspace(
  profile: Workspace["profile"],
  now = new Date(),
): Workspace {
  const people = [
    "Nimal Perera",
    "Anjali Silva",
    "Dinesh Fernando",
    "Sarah Jayawardena",
    "Ravi Kumar",
    "Amaya De Silva",
  ];
  const places = [
    "Colombo 07",
    "Kandy",
    "Galle",
    "Negombo",
    "Matara",
    "Colombo 03",
  ];
  return {
    version: 1,
    profile,
    preferences: { updates: true, offers: false },
    addresses: [
      { id: "home", label: "Home", address: "42 Flower Road, Colombo 07" },
      { id: "work", label: "Office", address: "15 Park Street, Colombo 02" },
    ],
    shipments: people.map((recipient, index) => ({
      id: `SD-${48291 - index * 119}`,
      recipient,
      phone: "+94 77 123 4567",
      destination: places[index],
      pickup: "42 Flower Road, Colombo 07",
      bookedAt: new Date(
        now.getTime() - [0, 1, 5, 9, 15, 22][index] * 86400000,
      ).toISOString(),
      status:
        index === 0
          ? "In transit"
          : index === 1
            ? "Awaiting pickup"
            : "Delivered",
      weight: 1 + index * 0.5,
      service: index % 2 ? "Standard" : "Express",
      packageType: "Small parcel",
      amount: [850, 650, 1200, 750, 1400, 850][index],
      notes: "",
    })),
  };
}
export function parseWorkspace(
  raw: string | null,
  fallback: Workspace,
): Workspace {
  if (!raw) return fallback;
  try {
    const parsed = workspaceSchema.safeParse(JSON.parse(raw));
    return parsed.success ? parsed.data : fallback;
  } catch {
    return fallback;
  }
}
export const estimatePrice = (weight: number, service: Shipment["service"]) =>
  450 + Math.ceil(weight) * 150 + (service === "Express" ? 350 : 0);
export const currency = (amount: number) =>
  `LKR ${amount.toLocaleString("en-LK")}`;
export const dateLabel = (value: string) =>
  new Date(value).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
  });
export const statusColor = (status: Shipment["status"]) =>
  ({
    "Awaiting pickup": "warning",
    "In transit": "info",
    Delivered: "success",
    Cancelled: "secondary",
  })[status];
export function trackingSteps(status: Shipment["status"]) {
  const completed =
    status === "Delivered" ? 4 : status === "In transit" ? 3 : 1;
  return [
    "Shipment created",
    "Parcel collected",
    "In transit",
    "Delivered",
  ].map((title, index) => ({
    title,
    complete: index < completed && status !== "Cancelled",
    description:
      status === "Cancelled"
        ? "Shipment cancelled"
        : index < completed
          ? "Confirmed in demo record"
          : "Not yet reached",
  }));
}
export function shipmentCsv(shipments: Shipment[]) {
  const cell = (value: string | number) => {
    let text = String(value);
    if (/^[\s]*[=+@-]/.test(text)) text = `'${text}`;
    return `"${text.replace(/"/g, '""')}"`;
  };
  return [
    [
      "Tracking number",
      "Recipient",
      "Destination",
      "Status",
      "Booked",
      "Amount (LKR)",
    ],
    ...shipments.map((s) => [
      s.id,
      s.recipient,
      s.destination,
      s.status,
      s.bookedAt,
      s.amount,
    ]),
  ]
    .map((row) => row.map(cell).join(","))
    .join("\r\n");
}
