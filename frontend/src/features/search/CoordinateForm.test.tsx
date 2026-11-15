import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithI18n } from '@/test/render';
import { CoordinateForm } from './CoordinateForm';

async function fill(values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    await userEvent.type(screen.getByLabelText(new RegExp(label)), value);
  }
  await userEvent.click(screen.getByRole('button', { name: /Use this rectangle/ }));
}

describe('CoordinateForm', () => {
  it('applies a valid rectangle (accepting the decimal comma)', async () => {
    const onApply = vi.fn();
    renderWithI18n(<CoordinateForm initial={null} onApply={onApply} />);
    await fill({ North: '41,5', South: '40', West: '10', East: '11.5' });
    expect(onApply).toHaveBeenCalledWith({ north: 41.5, south: 40, west: 10, east: 11.5 });
  });

  it.each([
    [{ North: '95', South: '40', West: '10', East: '11' }, /between -90 and 90/],
    [{ North: '40', South: '41', West: '10', East: '11' }, /North must be above south/],
    [{ North: '41', South: '40', West: '10' }, /all four fields/],
  ])('rejects invalid input %#', async (values, message) => {
    const onApply = vi.fn();
    renderWithI18n(<CoordinateForm initial={null} onApply={onApply} />);
    await fill(values);
    expect(screen.getByRole('alert')).toHaveTextContent(message);
    expect(onApply).not.toHaveBeenCalled();
  });
});
