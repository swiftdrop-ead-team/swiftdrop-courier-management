import { useEffect, useMemo, useState } from "react";
import { useTheme, alpha } from "@mui/material/styles";
import { Alert, Box, LinearProgress, ToggleButton, ToggleButtonGroup } from "@mui/material";
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler } from "chart.js";
import { Line, Doughnut } from "react-chartjs-2";
import { BarChart3, Download, Package, CheckCircle, TrendingUp, Clock, AlertCircle, ArrowUp, ArrowDown, Info } from "lucide-react";
import Table from "examples/Tables/Table";
import { WorkspacePagination } from "@/components/ui/WorkspacePagination";
import { adminService } from "@/services/authService";
import { createAdminPreview, downloadCsv } from "@/pages/admin/adminData";
import type { User } from "@/types/auth";
import type { Workspace } from "../useDeliveryWorkspace";
import { ActionButton, Field, Panel, SelectField, WorkspaceText as Text } from "../components";
import { day, labels, money, statuses, type ParcelStatus } from "../model";
import { comparison, inRange, permittedParcels, presetRange, previousRange, selectPeriod, summarize, trend, validRange, type Period } from "../analytics";
import "./analytics.css";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler);

/* ── Chart.js — Project Theme Defaults ── */
const chartTooltip: any = {
  backgroundColor: "rgba(0, 39, 76, 0.95)",
  titleColor: "#ffffff",
  bodyColor: "#e2e8f0",
  cornerRadius: 8,
  padding: 12,
  titleFont: { weight: "700", size: 12 },
  bodyFont: { size: 12 },
  displayColors: true,
  boxPadding: 4,
  boxWidth: 8,
  boxHeight: 8,
};
const chartLegend: any = {
  position: "bottom" as const,
  labels: {
    usePointStyle: true,
    padding: 20,
    font: { size: 12, weight: "600", family: "'Inter', 'Segoe UI', sans-serif" },
    color: "#64748b",
    boxWidth: 8,
    boxHeight: 8,
  },
};
const chartGrid: any = {
  color: "#f1f5f9",
  drawBorder: false,
  lineWidth: 1,
};
const chartTicks: any = {
  font: { size: 12, family: "'Inter', 'Segoe UI', sans-serif" },
  color: "#64748b",
};

/* ── Status colors — project semantic palette ── */
const rate = (value: number | null) => value === null ? "Not available" : `${value.toFixed(1)}%`;
const fee = (value: number, currency: string) => /^[A-Z]{3}$/.test(currency) ? money(value, currency) : `${value.toFixed(2)} (currency unavailable)`;

/* ── Change Badge Component ── */
function ChangeBadge({ current, previous }: { current: number; previous: number }) {
  if (!previous) return <span className="analytics-kpi__change analytics-kpi__change--neutral">No baseline</span>;
  const change = ((current - previous) / previous * 100);
  const isUp = change >= 0;
  return (
    <span className={`analytics-kpi__change ${isUp ? "analytics-kpi__change--up" : "analytics-kpi__change--down"}`}>
      {isUp ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
      {Math.abs(change).toFixed(1)}% vs previous
    </span>
  );
}

/* ── KPI Card Component ── */
function KPICard({ label, value, icon, color, change, note }: {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  color: "info" | "success" | "warning" | "danger" | "active" | "brand";
  change?: { current: number; previous: number };
  note?: string;
}) {
  return (
    <div className="analytics-kpi">
      <div className={`analytics-kpi__icon analytics-kpi__icon--${color}`}>{icon}</div>
      <span className="analytics-kpi__label">{label}</span>
      <h3 className={`analytics-kpi__value${value === "Not available" ? " analytics-kpi__value--unavailable" : ""}`}>{value}</h3>
      {change !== undefined && <ChangeBadge current={change.current} previous={change.previous} />}
      {note && <span className="analytics-kpi__note">{note}</span>}
    </div>
  );
}

/* ── Loading Skeleton ── */
function SkeletonKPIs() {
  return (
    <Box className="analytics-kpis">
      {[1, 2, 3, 4].map(i => <div key={i} className="analytics-skeleton analytics-skeleton--kpi" />)}
    </Box>
  );
}
function SkeletonCharts() {
  return (
    <Box className="analytics-chart-layout">
      <Box sx={{ p: 3 }}>
        <div className="analytics-skeleton analytics-skeleton--text" />
        <div className="analytics-skeleton analytics-skeleton--text-sm" />
        <div className="analytics-skeleton analytics-skeleton--chart" />
      </Box>
      <Box sx={{ p: 3 }}>
        <div className="analytics-skeleton analytics-skeleton--text" />
        <div className="analytics-skeleton analytics-skeleton--text-sm" />
        {[1, 2, 3, 4, 5].map(i => <div key={i} className="analytics-skeleton analytics-skeleton--bar" />)}
      </Box>
    </Box>
  );
}

/* ── Empty State Component ── */
function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <Box className="analytics-empty">
      <Box className="analytics-empty__icon"><BarChart3 size={28} /></Box>
      <Text variant="h6" fontWeight={700} sx={{ color: "#00274c" }}>{title}</Text>
      <Text variant="body2" sx={{ color: "#67748e", maxWidth: 360, lineHeight: 1.6 }}>{description}</Text>
    </Box>
  );
}

/* ── Account Growth Component ── */
function AccountGrowth({ from, to, preview }: { from: string; to: string; preview: boolean }) {
  const [users, setUsers] = useState<User[] | null>(null);
  const [error, setError] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setError(false); setUsers(null);
    if (preview) { setUsers(createAdminPreview().users); return; }
    adminService.getAllUsers().then(result => { if (active) setUsers(result); }).catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [preview, revision]);
  const range = { from, to }, previous = previousRange(range);
  const registered = (user: User, window: typeof range) => Number.isFinite(Date.parse(user.createdAt || "")) && inRange(day(user.createdAt), window);
  return <Panel title="Account growth" subtitle="New registrations in the selected period, compared with the preceding period of equal length.">
    {error ? <Alert severity="warning" action={<ActionButton onClick={() => setRevision(n => n + 1)}>Retry</ActionButton>}>Account growth could not be loaded.</Alert> : !users ? <LinearProgress aria-label="Loading account growth" /> :
      <Box className="analytics-growth">{(["CUSTOMER", "MERCHANT", "COURIER"] as const).map(role => {
        const current = users.filter(u => u.roles.includes(role) && registered(u, range)).length;
        const before = users.filter(u => u.roles.includes(role) && registered(u, previous)).length;
        return <Box key={role}>
          <span className="analytics-growth__label">New {role.toLowerCase()} accounts</span>
          <span className="analytics-growth__value">{current}</span>
          <span className="analytics-growth__change">{comparison(current, before)}</span>
        </Box>;
      })}</Box>}
    <Text variant="caption" display="block" mt={2}>Multi-role accounts appear in each applicable group. Records without a registration date are excluded.</Text>
  </Panel>;
}

export function Analytics({ workspace: w }: { workspace: Workspace }) {
  const { palette, typography } = useTheme();
  const chartPrimary = palette.secondary.main;
  const chartSecondary = palette.primary.main;
  const statusColors: Record<ParcelStatus, string> = {
    CREATED: palette.grey[500], PICKED_UP: palette.info.main,
    IN_TRANSIT: chartPrimary, OUT_FOR_DELIVERY: palette.warning.main,
    DELIVERED: palette.success.main, FAILED: palette.error.main, CANCELLED: palette.grey[300],
  };
  const themeStyle = {
    "--analytics-primary": chartPrimary,
    "--analytics-accent": chartSecondary,
    "--analytics-tint": alpha(chartSecondary, 0.14),
    "--analytics-ink": palette.brand.navy,
  };
  const tooltip = { ...chartTooltip, backgroundColor: palette.brand.navy,
    titleFont: { family: typography.fontFamily, size: 13 }, bodyFont: { family: typography.fontFamily, size: 13 } };
  const legend = { ...chartLegend, labels: { ...chartLegend.labels, font: { family: typography.fontFamily, size: 12 } } };
  const ticks = { ...chartTicks, font: { family: typography.fontFamily, size: 12 } };
  const [range, setRange] = useState(() => presetRange("month"));
  const [preset, setPreset] = useState<string>("month");
  const [period, setPeriod] = useState<Period>("day");
  const [metric, setMetric] = useState("volume");
  const [selectedCurrency, setCurrency] = useState("");
  const [page, setPage] = useState(0), [pageSize, setPageSize] = useState(10);
  const [performance, setPerformance] = useState("merchant");
  const [groupPage, setGroupPage] = useState(0), [groupSize, setGroupSize] = useState(5);
  const courier = w.role === "COURIER", admin = w.role === "ADMIN";
  const valid = validRange(range);
  const safeRange = valid ? range : presetRange("month");
  const data = useMemo(() => permittedParcels(w.parcels, w.role, w.ownerId), [w.parcels, w.role, w.ownerId]);
  const current = useMemo(() => selectPeriod(data, safeRange, courier), [data, safeRange.from, safeRange.to, courier]);
  const stats = useMemo(() => summarize(current), [current]);
  const priorRange = previousRange(safeRange);
  const prior = summarize(selectPeriod(data, priorRange, courier));
  const bins = useMemo(() => trend(data, safeRange, period, courier), [data, safeRange.from, safeRange.to, period, courier]);
  const currencies = [...new Set(current.map(p => p.currency || "Unknown currency"))].sort();
  const currency = currencies.includes(selectedCurrency) ? selectedCurrency : currencies[0] || "Unknown currency";
  const maxPage = Math.max(0, Math.ceil(bins.length / pageSize) - 1);
  const currentPage = Math.min(page, maxPage);
  const groups = useMemo(() => {
    const map = new Map<string, typeof current>();
    for (const p of current) {
      if (performance === "merchant" && p.ownerRole !== "MERCHANT") continue;
      const id = performance === "merchant" ? p.ownerId : p.courierId;
      if (id) map.set(id, [...(map.get(id) || []), p]);
    }
    return [...map].map(([id, records]) => ({ id, name: performance === "merchant" ? records[0].senderName || id : `Courier ${id}`, ...summarize(records) })).sort((a, b) => b.total - a.total);
  }, [current, performance]);
  const activeGroupPage = Math.min(groupPage, Math.max(0, Math.ceil(groups.length / groupSize) - 1));
  const columnNames = [courier ? "Assignment period" : "Booking period", "Shipments", "Delivered", ...(!courier ? [`Fees (${currency})`] : [])];
  const exportRows = [columnNames, ...bins.map(bin => [bin.label, bin.total, bin.delivered, ...(!courier ? [(bin.fees[currency] || 0).toFixed(2)] : [])])];

  // Status distribution data for donut chart
  const statusDistribution = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const s of statuses) counts[s] = 0;
    for (const p of current) counts[p.status] = (counts[p.status] || 0) + 1;
    return { labels: statuses.map(s => labels[s]), counts: statuses.map(s => counts[s] || 0), colors: statuses.map(s => statusColors[s]) };
  }, [current, palette]);

  // Guard: role check
  if (!["ADMIN", "MERCHANT", "COURIER", "CUSTOMER"].includes(w.role)) return <Alert severity="info">Analytics is available for operational roles.</Alert>;

  // Loading state with skeletons
  if (w.loading) return <Box className="analytics-page" sx={themeStyle} role="status">
    <Panel title="Loading analytics" subtitle="Fetching your delivery data...">
      <LinearProgress />
    </Panel>
    <SkeletonKPIs />
    <SkeletonCharts />
  </Box>;

  // Error state
  if (w.errors.parcels) return <Box className="analytics-page" sx={themeStyle}>
    <Panel title="Analytics unavailable">
      <EmptyState title="Unable to load analytics" description="Delivery records could not be loaded from the server. No statistics are shown until the connection is restored." />
      <ActionButton onClick={() => void w.refresh()} sx={{ mt: 2, display: "flex", justifyContent: "center" }}>Retry analytics</ActionButton>
    </Panel>
  </Box>;

  const changeRange = (key: "from" | "to", value: string) => { setRange(previous => ({ ...previous, [key]: value })); setPreset("custom"); setPage(0); setGroupPage(0); };

  return <Box className="analytics-page" sx={themeStyle}>
    {/* ── Filters Panel ── */}
    <Panel title="Reporting period" subtitle={courier ? "Your assigned deliveries, grouped by assignment date." : admin ? "Platform shipments, grouped by booking date." : w.role === "CUSTOMER" ? "Your personal shipments, grouped by booking date." : "Your business shipments, grouped by booking date."}
      actions={<ActionButton color="primary" variant="contained" startIcon={<Download size={16} />} disabled={!valid || !current.length} onClick={() => downloadCsv(`${w.role.toLowerCase()}-analytics.csv`, exportRows)}>Export analytics</ActionButton>}>
      <Box className="analytics-filters">
        <SelectField label="Date range" value={preset} options={[{value:"day",label:"Today"},{value:"week",label:"Last 7 days"},{value:"month",label:"This month"},{value:"year",label:"This year"},{value:"custom",label:"Custom range"}]}
          onChange={value => { setPreset(value); if (value !== "custom") { setRange(presetRange(value as Period)); setPeriod(value === "year" ? "month" : "day"); } setPage(0); setGroupPage(0); }} />
        <Field label="From date" type="date" value={range.from} onChange={e => changeRange("from", e.target.value)} />
        <Field label="To date" type="date" value={range.to} onChange={e => changeRange("to", e.target.value)} />
        <SelectField label="Group by" value={period} options={["day","week","month","year"].map(value=>({value,label:({day:"Daily",week:"Weekly",month:"Monthly",year:"Yearly"})[value]!}))} onChange={value => { setPeriod(value as Period); setPage(0); }} />
      </Box>
      {!valid && <Alert severity="error">Choose valid start and end dates, in order, within a ten-year range.</Alert>}
    </Panel>

    {valid && <>
      <Text variant="caption">Compared with {priorRange.from} – {priorRange.to}. Outcomes reflect current shipment states, not historical snapshots.</Text>

      {/* ── KPI Cards ── */}
      <Box className="analytics-kpis">
        <KPICard label={courier ? "Assigned deliveries" : "Shipments booked"} value={stats.total} icon={<Package size={22} />} color="info" change={{ current: stats.total, previous: prior.total }} />
        <KPICard label="Delivered" value={stats.delivered} icon={<CheckCircle size={22} />} color="success" change={{ current: stats.delivered, previous: prior.delivered }} />
        <KPICard label="Delivery success" value={rate(stats.success)} icon={<TrendingUp size={22} />} color="brand" note={stats.success !== null && prior.success !== null ? `${(stats.success - prior.success) >= 0 ? "+" : ""}${(stats.success - prior.success).toFixed(1)}pp vs previous` : "Delivered ÷ (delivered + failed)"} />
        {courier
          ? <KPICard label="Pending deliveries" value={stats.pending} icon={<Clock size={22} />} color="warning" note="Currently in progress in this cohort" />
          : <KPICard label="Cancelled" value={stats.cancelled} icon={<AlertCircle size={22} />} color="danger" note={`${stats.total ? (stats.cancelled / stats.total * 100).toFixed(1) : "0"}% of selected shipments`} />
        }
      </Box>

      {/* ── Empty State ── */}
      {!current.length ? <Panel title="No shipments in this period">
        <EmptyState title="No data for this period" description="Choose another date range to explore your delivery history, or create new shipments to see analytics here." />
      </Panel> : <>

        {/* ── Row 1: Trend Chart + Status Distribution ── */}
        <Box className="analytics-chart-layout">
          <Panel title={metric === "fees" && !courier ? `Delivery fee trend – ${currency}` : "Shipment trend"} subtitle="Volume and outcomes by period. Exact values are available in the trend data table."
            actions={!courier && <ToggleButtonGroup size="small" exclusive value={metric} onChange={(_, value) => { if(value) setMetric(value); }} aria-label="Trend metric"><ToggleButton value="volume">Shipments</ToggleButton><ToggleButton value="fees">Delivery fees</ToggleButton></ToggleButtonGroup>}>
            {!courier && metric === "fees" && <SelectField label="Currency" value={currency} options={currencies.map(value=>({value,label:value}))} onChange={setCurrency} />}
            <Box className="analytics-chart analytics-chart--tall" role="img" aria-label={`${metric === "fees" && !courier ? "Delivery fees" : "Shipment counts"} by ${period}. Exact values are available in Trend data below.`}>
              <Line
                data={{
                  labels: bins.map(bin => bin.label),
                  datasets: metric === "fees" && !courier ? [{
                    label: `Delivery fees (${currency})`,
                    data: bins.map(bin => bin.fees[currency] || 0),
                    borderColor: chartPrimary,
                    backgroundColor: alpha(chartPrimary, 0.05),
                    borderWidth: 2,
                    tension: 0.15,
                    fill: true,
                    pointBackgroundColor: "#ffffff",
                    pointBorderColor: chartPrimary,
                    pointBorderWidth: 2,
                    pointRadius: bins.length > 30 ? 0 : 2,
                    pointHoverRadius: 6,
                    pointHoverBorderWidth: 2,
                    pointHoverBackgroundColor: chartPrimary,
                    pointHoverBorderColor: "#ffffff",
                  }] : [{
                    label: courier ? "Assigned" : "Booked",
                    data: bins.map(bin => bin.total),
                    borderColor: chartPrimary,
                    backgroundColor: alpha(chartPrimary, 0.05),
                    borderWidth: 2,
                    tension: 0.15,
                    fill: true,
                    pointBackgroundColor: "#ffffff",
                    pointBorderColor: chartPrimary,
                    pointBorderWidth: 2,
                    pointRadius: bins.length > 30 ? 0 : 2,
                    pointHoverRadius: 6,
                    pointHoverBorderWidth: 2,
                    pointHoverBackgroundColor: chartPrimary,
                    pointHoverBorderColor: "#ffffff",
                  }, {
                    label: "Now delivered",
                    data: bins.map(bin => bin.delivered),
                    borderColor: chartSecondary,
                    backgroundColor: alpha(chartSecondary, 0.08),
                    borderWidth: 2,
                    tension: 0.15,
                    fill: true,
                    pointBackgroundColor: "#ffffff",
                    pointBorderColor: chartSecondary,
                    pointBorderWidth: 2,
                    pointRadius: bins.length > 30 ? 0 : 2,
                    pointHoverRadius: 6,
                    pointHoverBorderWidth: 2,
                    pointHoverBackgroundColor: chartSecondary,
                    pointHoverBorderColor: "#ffffff",
                  }]
                }}
                options={{
                  responsive: true,
                  maintainAspectRatio: false,
                  animation: false,
                  interaction: { mode: "index" as const, intersect: false },
                  plugins: {
                    tooltip,
                    legend,
                  },
                  scales: {
                    x: {
                      grid: { display: false },
                      ticks: { maxTicksLimit: 4, maxRotation: 0, ...ticks },
                    },
                    y: {
                      beginAtZero: true,
                      ticks: metric === "fees" ? ticks : { ...ticks, precision: 0, maxTicksLimit: 6 },
                      grid: chartGrid,
                    },
                  },
                }}
              />
            </Box>
          </Panel>

          {/* ── Status Distribution Donut + Status Legend ── */}
          <Panel title="Shipment status" subtitle="Current states of shipments in the selected period.">
            {current.length > 0 && <Box className="analytics-chart analytics-chart--donut" role="img" aria-label="Shipment status donut chart">
              <Doughnut
                data={{
                  labels: statusDistribution.labels,
                  datasets: [{
                    data: statusDistribution.counts,
                    backgroundColor: statusDistribution.colors,
                    borderWidth: 2,
                    borderColor: "#ffffff",
                    hoverOffset: 3,
                  }]
                }}
                options={{
                  responsive: true,
                  maintainAspectRatio: false,
                  animation: false,
                  cutout: "65%",
                  plugins: {
                    tooltip,
                    legend: { display: false },
                  },
                }}
              />
            </Box>}
            <Box className="analytics-outcomes-list">
              {statuses.map(status => {
                const count = current.filter(p => p.status === status).length;
                const pct = current.length ? (count / current.length * 100) : 0;
                return (
                  <Box key={status} className="analytics-outcome">
                    <Box className="analytics-outcome__header">
                      <span className="analytics-outcome__label">
                        <span className="analytics-outcome__dot" style={{ background: statusColors[status] }} />
                        {labels[status]}
                      </span>
                      <span className="analytics-outcome__count">{count} ({pct.toFixed(0)}%)</span>
                    </Box>
                  </Box>
                );
              })}
            </Box>
          </Panel>
        </Box>

        {/* ── Row 2: Timing + Fees/Performance ── */}
        <Box className="analytics-chart-layout--equal">
          <Panel title="Delivery timing" subtitle="Elapsed time from recorded pickup to delivery for completed shipments in this cohort.">
            <Box className="analytics-timing">
              <span className="analytics-timing__value">{stats.hours === null ? "—" : `${stats.hours.toFixed(1)}h`}</span>
              <Text className="analytics-timing__label">Average delivery time</Text>
              <Text className="analytics-timing__note">Across {stats.timed} of {stats.delivered} delivered shipments with valid timestamps.</Text>
              {stats.hours !== null && prior.hours !== null && (
                <Text className="analytics-timing__note">{(stats.hours - prior.hours) >= 0 ? "+" : ""}{(stats.hours - prior.hours).toFixed(1)}h vs previous period</Text>
              )}
              {stats.hours === null && <Text className="analytics-timing__note">Both pickup and delivery events are required.</Text>}
            </Box>
          </Panel>

          <Panel title={courier ? "Performance context" : "Delivery fees"} subtitle={courier ? "Your own assignment history only." : "Quoted fees for non-cancelled shipments. These are not sales revenue or settled payments."}>
            {courier ? (
              <Box className="analytics-metric-grid">
                <Box className="analytics-metric-card">
                  <Text className="analytics-metric-card__amount">{stats.failed}</Text>
                  <Text className="analytics-metric-card__label">Failed deliveries</Text>
                </Box>
                <Box className="analytics-metric-card">
                  <Text className="analytics-metric-card__amount">{stats.cancelled}</Text>
                  <Text className="analytics-metric-card__label">Cancelled assignments</Text>
                </Box>
              </Box>
            ) : (
              <Box className="analytics-metric-grid">
                {currencies.map(code => (
                  <Box key={code} className="analytics-metric-card">
                    <Text className="analytics-metric-card__amount">{fee(stats.fees[code] || 0, code)}</Text>
                    <Text className="analytics-metric-card__label">Total fees ({code})</Text>
                    <Text className="analytics-metric-card__change">{comparison(stats.fees[code] || 0, prior.fees[code] || 0)}</Text>
                  </Box>
                ))}
                {!currencies.length && <Text>No non-cancelled delivery fees in this period.</Text>}
              </Box>
            )}
          </Panel>
        </Box>

        {/* ── Row 3: Trend Data Table ── */}
        <Panel title="Trend data" subtitle="Detailed breakdown of shipment volumes and fees by period.">
          <Box component="details"><Box component="summary" className="analytics-table-summary"><BarChart3 size={18} />View accessible data table</Box>
            <Table aria-label="Analytics trend data" columns={columnNames.map((name, index) => ({ name, align: index === 0 ? "left" : "right" }))} rows={bins.slice(currentPage * pageSize, (currentPage + 1) * pageSize).map(bin => ({ id: bin.label, [columnNames[0]]: bin.label, Shipments: bin.total, Delivered: bin.delivered, ...(!courier ? { [columnNames[3]]: fee(bin.fees[currency] || 0, currency) } : {}) }))} />
            <WorkspacePagination count={bins.length} page={currentPage} pageSize={pageSize} onPageChange={setPage} onPageSizeChange={setPageSize} />
          </Box>
        </Panel>

        {/* ── Row 4: Admin Operational Performance ── */}
        {admin && <Panel title="Operational performance" subtitle="Shipment volume and current outcomes by merchant or assigned courier. Courier identifiers match assignment records."
          actions={<SelectField label="Compare" value={performance} options={[{ value: "merchant", label: "Merchants" }, { value: "courier", label: "Couriers" }]} onChange={value => { setPerformance(value); setGroupPage(0); }} />}>
          {groups.length ? <>
            <Table aria-label="Operational performance" columns={["Account", "Shipments", "Delivered", "Failed", "Success"].map((name, i) => ({ name, align: i ? "right" : "left" }))} rows={groups.slice(activeGroupPage * groupSize, (activeGroupPage + 1) * groupSize).map(group => ({ id: group.id, Account: <Box component="span"><Text component="span" display="block" fontWeight={700}>{group.name}</Text>{performance === "merchant" && <Text component="span" variant="caption">{group.id}</Text>}</Box>, Shipments: group.total, Delivered: group.delivered, Failed: group.failed, Success: rate(group.success) }))} />
            <WorkspacePagination count={groups.length} page={activeGroupPage} pageSize={groupSize} onPageChange={setGroupPage} onPageSizeChange={setGroupSize} />
          </> : <EmptyState title="No matching accounts" description="No merchants or couriers have shipments in this period." />}
        </Panel>}
      </>}

      {/* ── Admin Account Growth ── */}
      {admin && <AccountGrowth from={range.from} to={range.to} preview={w.preview} />}

      {/* ── Info Note ── */}
      <Box className="analytics-info">
        <Info size={18} className="analytics-info__icon" />
        <Text variant="body2" sx={{ color: "#67748e", lineHeight: 1.6 }}>
          {courier ? "Only your assignments are included. Shipments without an assignment date are excluded; missing timing events are not estimated."
            : admin ? "Delivery fees are available; recognized revenue, payment settlements and product sales are not tracked by these records."
            : w.role === "CUSTOMER" ? "Only your own bookings are included. Fees are quoted delivery charges; refunds and settled payments are not available in these records."
            : "These insights cover your shipments. Product sales, product rankings, sales revenue and average order value are not available in the current data."}
        </Text>
      </Box>
    </>}
  </Box>;
}
