import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';
import { installFakeApi, problem, type RecordedCall } from './test/fakeApi';
import { renderWithClient } from './test/render';

// Leaflet ha bisogno di un vero layout: la mappa e' verificata nel browser, qui si sostituisce
vi.mock('@/features/search/AreaMap', () => ({ AreaMap: () => <div data-testid="area-map" /> }));
vi.mock('@/features/detail/HotspotMap', () => ({
  HotspotMap: () => <div data-testid="hotspot-map" />,
}));

const calendarCalls = (calls: RecordedCall[]) => calls.filter((c) => c.path === '/api/v1/calendar');

beforeEach(() => {
  window.history.replaceState(null, '', '/');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('App', () => {
  it('shows the calendar with the default search and warns that the method is provisional', async () => {
    const calls = installFakeApi();
    renderWithClient(<App />);

    expect(await screen.findByRole('grid', { name: /Fire activity calendar/ })).toBeInTheDocument();
    expect(screen.getByText('Provisional harmonization method')).toBeInTheDocument();
    expect(calendarCalls(calls)[0]!.body).toEqual({
      boundaryCode: 'Argentina',
      mode: 'HARMONIZED',
      dayNight: 'ANY',
      minConfidence: 'NOMINAL',
      sourceType: 'VEGETATION',
    });
  });

  it('asks again with the new filter and keeps it in the URL', async () => {
    const calls = installFakeApi();
    renderWithClient(<App />);
    await screen.findByRole('grid');

    await userEvent.click(screen.getByRole('radio', { name: 'Raw' }));

    await waitFor(() => expect(calendarCalls(calls)).toHaveLength(2));
    expect(calendarCalls(calls)[1]!.body).toMatchObject({ mode: 'RAW' });
    expect(window.location.search).toBe('?mode=RAW');
  });

  it('opens the day-by-day detail of a month and puts it in the URL', async () => {
    const calls = installFakeApi();
    renderWithClient(<App />);

    await userEvent.click(await screen.findByRole('button', { name: /January 2019/ }));

    expect(
      await screen.findByRole('heading', { name: 'Day by day: January 2019' }),
    ).toBeInTheDocument();
    expect(window.location.search).toBe('?month=2019-01');
    expect(calls.find((c) => c.path === '/api/v1/month-detail')!.body).toMatchObject({
      year: 2019,
      month: 1,
    });

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('heading', { name: /Day by day/ })).not.toBeInTheDocument();
  });

  it('restores the view from the URL', async () => {
    window.history.replaceState(null, '', '/?area=Italy&month=2019-01');
    const calls = installFakeApi();
    renderWithClient(<App />);

    expect(
      await screen.findByRole('heading', { name: 'Day by day: January 2019' }),
    ).toBeInTheDocument();
    expect(calendarCalls(calls)[0]!.body).toMatchObject({ boundaryCode: 'Italy' });
  });

  it('explains an API error in plain words and offers a retry for server failures', async () => {
    let fail = true;
    installFakeApi((call) =>
      call.path === '/api/v1/calendar' && fail
        ? problem(503, 'Service unavailable', 'Try again soon')
        : undefined,
    );
    renderWithClient(<App />);

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Service unavailable')).toBeInTheDocument();

    fail = false;
    await userEvent.click(within(alert).getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('grid')).toBeInTheDocument();
  });

  it('does not offer a retry for an unknown area (a 4xx will not fix itself)', async () => {
    installFakeApi((call) =>
      call.path === '/api/v1/calendar' ? problem(404, 'Unknown area', 'Atlantide') : undefined,
    );
    renderWithClient(<App />);

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Unknown area')).toBeInTheDocument();
    expect(within(alert).queryByRole('button')).not.toBeInTheDocument();
  });

  it('speaks English by default and switches to Italian, remembering the choice', async () => {
    window.localStorage.clear();
    installFakeApi();
    renderWithClient(<App />);

    expect(await screen.findByRole('heading', { name: 'Fire calendar' })).toBeInTheDocument();
    expect(document.documentElement.lang).toBe('en');

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'it');

    expect(
      await screen.findByRole('heading', { name: 'Calendario degli incendi' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /gennaio 2019: 1\.200 hotspot/ }),
    ).toBeInTheDocument();
    expect(document.documentElement.lang).toBe('it');
    expect(window.localStorage.getItem('firecal.locale')).toBe('it');
  });

  it('asks the server for texts in the chosen language and rereads them when it changes', async () => {
    const calls = installFakeApi();
    renderWithClient(<App />);
    await screen.findByRole('grid');
    expect(calls.every((c) => c.language === 'en')).toBe(true);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'it');

    await waitFor(() => expect(calendarCalls(calls).map((c) => c.language)).toEqual(['en', 'it']));
    expect(calls.filter((c) => c.path === '/api/v1/metadata').map((c) => c.language)).toEqual([
      'en',
      'it',
    ]);
  });

  it('starts in Italian when the saved choice or the link says so', async () => {
    window.history.replaceState(null, '', '/?lang=it');
    installFakeApi();
    renderWithClient(<App />, { locale: 'detect' });
    expect(
      await screen.findByRole('heading', { name: 'Calendario degli incendi' }),
    ).toBeInTheDocument();
  });
});
