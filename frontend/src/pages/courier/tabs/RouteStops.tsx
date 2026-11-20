import { Box } from "@mui/material";
import { ActionButton as SoftButton, Status, WorkspaceText as Typography } from "@/features/delivery/components";
import type { ParcelRecord } from "@/features/delivery/model";

export function RouteStops({ parcels, onOpen }: {
  parcels: ParcelRecord[];
  onOpen: (parcel: ParcelRecord) => void;
}) {
  return (
    <Box component="ul" sx={{ display: "grid", gap: 1.5, listStyle: "none", p: 0, m: 0 }}>
          {parcels.map((parcel) => (
            <Box component="li" key={parcel.id} sx={{ border: "1px solid #dfe6ee", borderRadius: 2, p: 2 }}>
              <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "space-between", gap: 1 }}>
                <Typography fontWeight={700}>{parcel.trackingNumber}</Typography>
                <Status value={parcel.status} />
              </Box>
              <Box sx={{ display: "grid", gridTemplateColumns: { sm: "1fr 1fr" }, gap: 1.5, my: 2 }}>
                <Box><Typography variant="caption" color="text.secondary">Pickup</Typography>
                  <Typography variant="body2">{parcel.pickupAddress}</Typography></Box>
                <Box><Typography variant="caption" color="text.secondary">Drop-off for {parcel.recipientName}</Typography>
                  <Typography variant="body2">{parcel.dropoffAddress}</Typography></Box>
              </Box>
              <SoftButton aria-label={`View ${parcel.trackingNumber}`} onClick={() => onOpen(parcel)}>View details</SoftButton>
            </Box>
          ))}
        </Box>
  );
}
