import { screen } from '@testing-library/react';
import { renderWithI18n } from '@/test/render';
import type { HeatCell } from '@/domain/heatmap';
import { CellInspector } from './CellInspector';

const cell: HeatCell = {
  year: 2019,
  month: 8,
  value: 27701,
  provisional: false,
  incomplete: false,
  uncertainty: 0.14,
  ratio: 0.76,
};

describe('CellInspector', () => {
  it('shows the declared uncertainty of the month in the chosen language', () => {
    renderWithI18n(<CellInspector cell={cell} bySatellite={{}} modeLabel="harmonized" />);
    expect(screen.getByText('±14% (error below this in 8 cases out of 10)')).toBeInTheDocument();
  });

  it('says nothing about uncertainty when the API does not know it', () => {
    renderWithI18n(
      <CellInspector cell={{ ...cell, uncertainty: null }} bySatellite={{}} modeLabel="raw" />,
      { locale: 'it' },
    );
    expect(screen.queryByText(/errore sotto/)).not.toBeInTheDocument();
  });
});
