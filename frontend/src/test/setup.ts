import '@testing-library/jest-dom';
import { configure } from '@testing-library/react';

// Allow MUI dialog exit transitions to finish on constrained Windows/CI hosts.
configure({ asyncUtilTimeout: 5000 });
