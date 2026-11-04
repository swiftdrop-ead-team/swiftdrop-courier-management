// The MIT-licensed template ships JavaScript with runtime PropTypes.
// Keep its public modules usable from SwiftDrop's TypeScript application.
declare module 'components/*' {
  const Component: import('react').ComponentType<any>;
  export default Component;
}
declare module 'examples/*' {
  const Component: import('react').ComponentType<any>;
  export default Component;
}
declare module 'assets/theme' {
  const theme: import('@mui/material/styles').Theme;
  export default theme;
}
declare module 'context' {
  export const SoftUIControllerProvider: import('react').ComponentType<import('react').PropsWithChildren>;
  export function useSoftUIController(): [Record<string, any>, import('react').Dispatch<any>];
  export function setMiniSidenav(dispatch: import('react').Dispatch<any>, value: boolean): void;
}
