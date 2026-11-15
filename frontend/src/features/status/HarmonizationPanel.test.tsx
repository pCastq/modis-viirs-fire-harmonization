import { screen, waitFor, within } from '@testing-library/react';
import { DEFAULT_QUERY, type SearchQuery } from '@/domain/query';
import { installFakeApi, problem } from '@/test/fakeApi';
import { renderWithClient } from '@/test/render';
import { HarmonizationPanel } from './HarmonizationPanel';

afterEach(() => {
  vi.unstubAllGlobals();
});

const polygon: SearchQuery = {
  ...DEFAULT_QUERY,
  area: {
    kind: 'polygon',
    ring: [
      [10, 40],
      [11, 40],
      [11, 41],
    ],
  },
};

describe('HarmonizationPanel', () => {
  it('shows the multiplier of every quarter and time of day, with the calibration years', async () => {
    installFakeApi();
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />);

    const table = await screen.findByRole('table', {
      name: 'Multiplier k by quarter and time of day',
    });
    const first = within(table).getByRole('row', { name: /Jan–Mar/ });
    // gennaio-marzo: tutto il giorno k = 1, giorno 1,25, notte 3,5
    expect(within(first).getByText('×1.00')).toBeInTheDocument();
    expect(within(first).getByText('×1.25')).toBeInTheDocument();
    expect(within(first).getByText('×3.50')).toBeInTheDocument();
    expect(
      screen.getByText(/Calibrated on January 2012 – December 2018 \(83 complete months\)/),
    ).toBeInTheDocument();
  });

  it('marks a multiplier borrowed from a coarser level and explains it', async () => {
    installFakeApi();
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />);

    const borrowed = await screen.findByText('×4.50*');
    expect(borrowed).toHaveAttribute('title', expect.stringContaining('whole calibration'));
    expect(screen.getByText(/Marked values: too little data/)).toBeInTheDocument();
  });

  it('declares the units and the uncertainty bands', async () => {
    installFakeApi();
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />);

    expect(await screen.findByText(/k counts cell-days/)).toBeInTheDocument();
    expect(screen.getByText('<10 cell-days: ±100%')).toBeInTheDocument();
    expect(screen.getByText('1000+ cell-days: ±19%')).toBeInTheDocument();
  });

  it('writes the panel in Italian with Italian decimals', async () => {
    installFakeApi();
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />, { locale: 'it' });

    expect(
      await screen.findByText('Come MODIS e VIIRS diventano confrontabili'),
    ).toBeInTheDocument();
    expect((await screen.findAllByText('×1,25')).length).toBeGreaterThan(0);
  });

  it('asks the API for the chosen country and filters, in the chosen language', async () => {
    const calls = installFakeApi();
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />, { locale: 'it' });
    await screen.findByRole('table');

    const call = calls.find((c) => c.path.startsWith('/api/v1/harmonization?'));
    expect(call?.path).toBe(
      '/api/v1/harmonization?boundaryCode=Argentina&sourceType=VEGETATION&minConfidence=NOMINAL',
    );
    expect(call?.language).toBe('it');
  });

  it('says there is no calibration instead of inventing multipliers', async () => {
    installFakeApi((call) =>
      call.path.startsWith('/api/v1/harmonization?')
        ? new Response(
            JSON.stringify({
              method: { id: 'country-quarter-k', version: '1' },
              calibration: null,
              area: 'Argentina',
              satelliteGroup: 'TERRA_AQUA',
              sourceType: 'VEGETATION',
              minConfidence: 'NOMINAL',
              factors: [1, 2, 3, 4].map((quarter) => ({
                quarter,
                dayNight: 'ANY',
                k: null,
                source: null,
                viirsCellDays: 0,
                modisCellDays: 0,
              })),
              uncertaintyBands: [],
              notes: [],
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          )
        : undefined,
    );
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />);

    expect(await screen.findByText(/No calibration yet/)).toBeInTheDocument();
    expect(screen.queryByText(/×/)).not.toBeInTheDocument();
  });

  it('does not ask the API for a drawn area and explains why', () => {
    const calls = installFakeApi();
    renderWithClient(<HarmonizationPanel query={polygon} />);

    expect(screen.getByText(/A drawn area has no single multiplier/)).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('shows a readable error when the API fails', async () => {
    installFakeApi((call) =>
      call.path.startsWith('/api/v1/harmonization?')
        ? problem(404, 'Not found', 'Unknown country')
        : undefined,
    );
    renderWithClient(<HarmonizationPanel query={DEFAULT_QUERY} />);

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
