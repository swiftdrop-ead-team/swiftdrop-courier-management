import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import theme from "assets/theme";
import { DeliveryDashboard } from "../features/delivery/DeliveryDashboard";
import { deliveryApi } from "../features/delivery/service";
import { adminService } from "../services/authService";
import { day, emptyBooking, type ParcelRecord } from "../features/delivery/model";
import type { UserRole } from "../types/auth";
vi.mock("react-chartjs-2",()=>({Line:()=> <div data-testid="trend-chart"/>, Doughnut:()=> <div data-testid="status-chart"/>}));
vi.mock("../hooks/useAuth",()=>({useAuth:()=>({user:{id:"self",fullName:"Self",roles:["ADMIN","MERCHANT","COURIER"],merchantApprovalStatus:"APPROVED"}})}));
const record: ParcelRecord={...emptyBooking(),id:"parcel",trackingNumber:"SD-SELF",ownerId:"self",ownerRole:"MERCHANT",courierId:"self",assignedDate:day(),createdAt:new Date().toISOString(),status:"DELIVERED",amount:250,currency:"LKR",history:[]};
function open(role:UserRole){return render(<ThemeProvider theme={theme}><MemoryRouter initialEntries={["/workspace/analytics"]}><Routes><Route path="/workspace/:tab" element={<DeliveryDashboard role={role}/>}/></Routes></MemoryRouter></ThemeProvider>);}
beforeEach(()=>{vi.restoreAllMocks();vi.spyOn(adminService,"getAllUsers").mockResolvedValue([]);});
it.each([["ADMIN","all",2],["MERCHANT","mine",1],["COURIER","assigned",1],["CUSTOMER","mine",1]] as const)("loads %s analytics through the permitted endpoint",async(role,scope,count)=>{
  const load=vi.spyOn(deliveryApi,"parcels").mockResolvedValue([record,{...record,id:"other",ownerId:"other",courierId:"other"}]);
  open(role);
  const label=await screen.findByText(role==="COURIER"?"Assigned deliveries":"Shipments booked");
  expect(within(label.parentElement!).getByRole("heading",{level:3})).toHaveTextContent(String(count));
  expect(load).toHaveBeenCalledWith(scope,expect.any(AbortSignal));
  expect(screen.getByTestId("trend-chart")).toBeInTheDocument();
  if(role!=="ADMIN") expect(adminService.getAllUsers).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText("From date"),{target:{value:"2099-01-01"}});
  expect(screen.getByText(/Choose valid start and end dates/)).toBeInTheDocument();
  expect(screen.queryByTestId("trend-chart")).not.toBeInTheDocument();
});
it("shows an error instead of zero statistics after a failed data load",async()=>{
  vi.spyOn(deliveryApi,"parcels").mockRejectedValue(new Error("Offline"));open("MERCHANT");
  expect(await screen.findByText("Analytics unavailable")).toBeInTheDocument();
  expect(screen.queryByText("Shipments booked")).not.toBeInTheDocument();
  expect(screen.getByRole("button",{name:"Retry analytics"})).toBeInTheDocument();
});
it("shows a meaningful empty state for real empty results",async()=>{
  vi.spyOn(deliveryApi,"parcels").mockResolvedValue([]);open("MERCHANT");
  expect(await screen.findByText("No shipments in this period")).toBeInTheDocument();
  expect(screen.queryByTestId("trend-chart")).not.toBeInTheDocument();
});
