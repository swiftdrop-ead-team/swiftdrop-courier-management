import "@mui/material/styles";

declare module "@mui/material/styles" {
  interface Palette {
    brand: {
      navy: string;
      blue: string;
      yellow: string;
      yellowLight: string;
      surface: string;
      surfaceRaised: string;
      border: string;
      text: string;
      textMuted: string;
      focusRing: string;
    };
  }

  interface PaletteOptions {
    brand?: Palette["brand"];
  }

  interface Theme {
    functions: {
      rgba: (color: string, opacity: number) => string;
      linearGradient: (
        color: string,
        colorState: string,
        angle?: number,
      ) => string;
    };
  }

  interface ThemeOptions {
    functions?: Theme["functions"];
  }
}
