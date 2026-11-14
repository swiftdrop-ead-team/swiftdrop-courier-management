import { authService } from "../../services/authService";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import { useAuth } from "../../hooks/useAuth";
import type { UserRole } from "../../types/auth";
import { getPreview, subscribePreview } from "./preview";
import {
  previewOwner,
  type Courier,
  type Integration,
  type Notice,
  type ParcelRecord,
  type Profile,
  type SavedAddress,
  type WebhookAttempt,
  type Zone,
} from "./model";
import { deliveryApi, errorMessage } from "./service";

interface LiveData {
  parcels: ParcelRecord[];
  couriers: Courier[];
  zones: Zone[];
  notices: Notice[];
  integration: Integration | null;
  attempts: WebhookAttempt[];
  profile: Profile | null;
  addresses: SavedAddress[];
}
const empty: LiveData = {
  parcels: [],
  couriers: [],
  zones: [],
  notices: [],
  integration: null,
  attempts: [],
  profile: null,
  addresses: [],
};
export function useDeliveryWorkspace(
  role: UserRole,
  preview: boolean,
  tab = "overview",
) {
  const { user, refreshUser } = useAuth();
  const ownerId = preview ? previewOwner(role) : user?.id || "";
  // Do not read or seed preview storage in authenticated workspaces.
  const snapshot = useSyncExternalStore(
    preview ? subscribePreview : () => () => {},
    preview ? getPreview : () => null,
  );
  const [live, setLive] = useState<LiveData>(empty);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(!preview);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const controller = useRef<AbortController>();
  const lock = useRef(false);
  const mounted = useRef(true);
  const refresh = useCallback(async () => {
    if (preview) return;
    controller.current?.abort();
    const request = new AbortController();
    controller.current = request;
    const jobs: [keyof LiveData, () => Promise<unknown>][] = [];
    if (
      [
        "overview",
        "deliveries",
        "dispatch",
        "reports",
        "history",
        "route",
        "shipments",
        "shipment-detail",
      ].includes(tab)
    )
      jobs.push([
        "parcels",
        () =>
          deliveryApi.parcels(
            role === "ADMIN" ? "all" : role === "COURIER" ? "assigned" : "mine",
            request.signal,
          ),
      ]);
    if (["profile", "settings", "send", "create-shipment"].includes(tab)) {
      jobs.push(["profile", () => deliveryApi.profile(request.signal)]);
      if (role === "CUSTOMER" || role === "MERCHANT")
        jobs.push(["addresses", () => deliveryApi.addresses(request.signal)]);
    }
    if (
      (role === "ADMIN" && tab === "dispatch") ||
      (role === "COURIER" && tab === "overview")
    )
      jobs.push(["couriers", () => deliveryApi.couriers(request.signal)]);
    if (role !== "ADMIN" && tab === "notifications")
      jobs.push(["notices", () => deliveryApi.notices(request.signal)]);
    if (role === "MERCHANT" && tab === "api") {
      try {
        const user = await authService.getCurrentUser();
        if (user.merchantApprovalStatus === "APPROVED")
        jobs.push(
          ["integration", () => deliveryApi.integration(request.signal)],
          ["attempts", () => deliveryApi.attempts(request.signal)],
        );
      } catch (error) {
        if (request.signal.aborted) return;
        setLive(empty);
        setErrors({ account: errorMessage(error) });
        setLoading(false);
        return;
      }
    }
    const results = await Promise.allSettled(jobs.map(([, job]) => job()));
    if (request.signal.aborted) return;
    const nextErrors: Record<string, string> = {};
    const patch: Partial<LiveData> = {};
    results.forEach((result, index) => {
      const key = jobs[index][0];
      if (result.status === "fulfilled")
        Object.assign(patch, { [key]: result.value });
      else {
        Object.assign(patch, { [key]: empty[key] });
        nextErrors[key] = errorMessage(result.reason);
      }
    });
    setLive((previous) => ({ ...previous, ...patch }));
    setErrors(nextErrors);
    setLoading(false);
  }, [preview, role, tab]);
  useEffect(() => {
    mounted.current = true;
    setLive(empty);
    setErrors({});
    setLoading(!preview);
    void refresh();
    const interval = preview
      ? undefined
      : window.setInterval(() => {
          if (!document.hidden && !lock.current) void refresh();
        }, 20000);
    return () => {
      mounted.current = false;
      controller.current?.abort();
      if (interval) window.clearInterval(interval);
    };
  }, [refresh, preview, ownerId]);
  const act = async (
    operation: () => void | Promise<unknown>,
    success: string,
  ) => {
    if (lock.current) return false;
    lock.current = true;
    setBusy(true);
    setActionError("");
    try {
      await operation();
      if (!mounted.current) return true;
      setMessage(success);
      await refresh();
      return true;
    } catch (error) {
      if (mounted.current) setActionError(errorMessage(error));
      return false;
    } finally {
      lock.current = false;
      if (mounted.current) setBusy(false);
    }
  };
  const fallback: Profile = {
    fullName: preview
      ? `${role[0]}${role.slice(1).toLowerCase()} Preview`
      : user?.fullName || "",
    email: preview ? `${role.toLowerCase()}@example.com` : user?.email || "",
    phone: user?.phoneNumber || "",
    businessName: "",
    address: "",
    emailNotifications: true,
  };
  const parcels = snapshot
    ? snapshot.parcels.filter(
        (item) =>
          role === "ADMIN" ||
          (role === "COURIER"
            ? item.courierId === ownerId
            : item.ownerId === ownerId),
      )
    : live.parcels;
  return {
    role,
    preview,
    user,
    ownerId,
    parcels,
    couriers: snapshot?.couriers || live.couriers,
    zones: snapshot?.zones || live.zones,
    notices: snapshot
      ? snapshot.notices.filter((item) => item.ownerId === ownerId)
      : live.notices,
    integration: snapshot
      ? snapshot.integrations[ownerId] || null
      : live.integration,
    attempts: snapshot ? snapshot.webhooks[ownerId] || [] : live.attempts,
    profile: snapshot?.profiles[ownerId] || live.profile || fallback,
    addresses: snapshot ? snapshot.addresses[ownerId] || [] : live.addresses,
    errors,
    loading,
    busy,
    message,
    setMessage,
    actionError,
    setActionError,
    act,
    refresh,
    refreshAccount: () => refreshUser?.(),
  };
}
export type Workspace = ReturnType<typeof useDeliveryWorkspace>;
