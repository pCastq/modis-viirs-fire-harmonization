import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { buildCells } from '@/domain/heatmap';
import { calendar } from '@/test/fixtures';
import { renderWithI18n } from '@/test/render';
import { CalendarHeatmap } from './CalendarHeatmap';

function setup(selectedMonth: string | null = null) {
  const onSelect = vi.fn();
  const onInspect = vi.fn();
  renderWithI18n(
    <CalendarHeatmap
      cells={buildCells(calendar)}
      months={calendar.months}
      view="counts"
      max={1200}
      selectedMonth={selectedMonth}
      onSelect={onSelect}
      onInspect={onInspect}
    />,
  );
  return { onSelect, onInspect };
}

describe('CalendarHeatmap', () => {
  it('describes every cell for screen readers, including incomplete and missing months', () => {
    setup();
    expect(
      screen.getByRole('button', { name: 'January 2019: 1,200 hotspots' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'March 2019: 300 hotspots, incomplete month' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'February 2019: no data' })).toBeDisabled();
  });

  it('selects a month on click, but not one without data', async () => {
    const { onSelect } = setup();
    await userEvent.click(screen.getByRole('button', { name: /January 2019/ }));
    expect(onSelect).toHaveBeenCalledWith(2019, 1);
    await userEvent.click(screen.getByRole('button', { name: /February 2019/ }));
    expect(onSelect).toHaveBeenCalledTimes(1);
  });

  it('has a single tab stop and moves with the arrow keys', async () => {
    setup();
    const buttons = screen.getAllByRole('button');
    expect(buttons.filter((b) => b.tabIndex === 0)).toHaveLength(1);
    buttons[0]!.focus();
    await userEvent.keyboard('{ArrowRight}{ArrowRight}');
    expect(screen.getByRole('button', { name: /March 2019/ })).toHaveFocus();
  });

  it('marks the selected month', () => {
    setup('2019-01');
    expect(screen.getByRole('button', { name: /January 2019/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('shows the year total', () => {
    setup();
    expect(screen.getByRole('gridcell', { name: '1.5K' })).toBeInTheDocument();
  });
});
