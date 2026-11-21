import { WorkspaceText as Typography } from "@/features/delivery/components";
import { useState } from "react";
import { Box, Checkbox, FormControlLabel } from "@mui/material";
import { ActionButton as SoftButton } from "@/features/delivery/components";
import { time } from "@/features/delivery/model";
import { updatePreview } from "@/features/delivery/preview";
import { deliveryApi } from "@/features/delivery/service";
import type { Workspace } from "@/features/delivery/useDeliveryWorkspace";
import { Empty, Panel } from "@/features/delivery/components";

export function Notifications({ workspace: w }: { workspace: Workspace }) {
  const [unread, setUnread] = useState(false);
  const notices = w.notices.filter((item) => !unread || !item.read);
  return (
    <Panel
      actions={<FormControlLabel
        label="Show unread only"
        control={
          <Checkbox
            checked={unread}
            onChange={(_, checked) => setUnread(checked)}
          />
        }
      />}
      title="Delivery updates"
      subtitle={
        w.preview
          ? "Sample delivery updates for this preview."
          : "Updates and notification history for your account."
      }
    >
      {notices.map((item) => (
        <Box
          key={item.id}
          sx={{
            p: 2,
            borderRadius: 2,
            bgcolor: item.read ? "transparent" : "#f3f7fc",
            borderBottom: 1,
            borderColor: "brand.border",
            display: "flex",
            gap: 2,
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          <Box>
            <Typography variant="body2" fontWeight={item.read ? 400 : 700}>
              {item.text}
            </Typography>
            <Typography variant="caption">{time(item.timestamp)}</Typography>
          </Box>
          {!item.read && (
            <SoftButton
              variant="text"
              color="dark"
              disabled={w.busy}
              onClick={() =>
                void w.act(async () => {
                  if (w.preview)
                    updatePreview((state) => {
                      const notice = state.notices.find(
                        (entry) => entry.id === item.id,
                      );
                      if (notice) notice.read = true;
                    });
                  else await deliveryApi.readNotice(item.id);
                }, "Notification marked as read.")
              }
            >
              Mark read
            </SoftButton>
          )}
        </Box>
      ))}
      {!notices.length && (
        <Empty text={unread ? "You have no unread notifications." : "No notifications to show. New delivery updates will appear here."} />
      )}
    </Panel>
  );
}
