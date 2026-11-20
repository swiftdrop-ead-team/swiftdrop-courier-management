import { Box, Button, Pagination, PaginationItem } from "@mui/material";
import { useId } from "react";

type Props = {
  count: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
};

export function WorkspacePagination({ count, page, pageSize, onPageChange, onPageSizeChange }: Props) {
  const id = useId();
  const pages = Math.max(1, Math.ceil(count / pageSize));
  return <Box className="workspace-pagination">
    <Box className="workspace-pagination-size">
      <label htmlFor={id}>Rows per page</label>
      <select id={id} value={pageSize} onChange={(event) => {
        onPageSizeChange(Number(event.target.value));
        onPageChange(0);
      }}>{[5, 10, 25].map((size) => <option key={size} value={size}>{size}</option>)}</select>
    </Box>
    <span role="status">Showing {count ? page * pageSize + 1 : 0}–{Math.min((page + 1) * pageSize, count)} of {count}</span>
    <Box className="workspace-pagination-pages">
      <Button aria-label="Go to previous page" disabled={page === 0} onClick={() => onPageChange(page - 1)}>Previous</Button>
      <Pagination aria-label="Table pages" count={pages} page={page + 1} siblingCount={0} hidePrevButton hideNextButton
        onChange={(_, next) => onPageChange(next - 1)}
        renderItem={(item) => <PaginationItem {...item} />} />
      <Button aria-label="Go to next page" disabled={page >= pages - 1} onClick={() => onPageChange(page + 1)}>Next</Button>
    </Box>
  </Box>;
}
