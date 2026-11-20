import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged, forkJoin, tap, timer } from 'rxjs';
import { KafkaTradePayload, Query, Summary, Trade, TradeApiService, TradeSortField } from './trade-api.service';

type Theme = 'dark' | 'light';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="dashboard" [class.theme-light]="theme === 'light'">
      <header class="topbar">
        <a class="brand" href="/" aria-label="TradeFlow dashboard">
          <span class="brand-icon">⌁</span>
          <span class="brand-copy"><strong>TradeFlow</strong><small>CAPITAL MARKETS · FRONT OFFICE</small></span>
        </a>
        <div class="topbar-right">
          <span class="market-open"><i></i> MARKET OPEN</span>
          <span class="live-status"><i [class.offline]="!connected"></i>{{ connected ? 'LIVE UPDATES' : 'CONNECTING' }}</span>
          <div class="clock"><span>{{ now | date:'HH:mm:ss':'+0530' }}</span></div>
          <button class="icon-button" aria-label="Refresh trades" title="Refresh trades" (click)="load()" [disabled]="loading">⟳</button>
          <button class="theme-button" (click)="toggleTheme()" [attr.aria-label]="'Switch to ' + (theme === 'dark' ? 'light' : 'dark') + ' theme'">
            <span>{{ theme === 'dark' ? '☼' : '☾' }}</span> {{ theme === 'dark' ? 'Dark' : 'Light' }}
          </button>
        </div>
      </header>

      <section class="summary-grid" aria-label="Trade summary">
        <button class="summary-card card-violet" (click)="showAllTrades()">
          <span class="summary-top"><span class="summary-label">TOTAL TRADES</span><span class="card-icon">◈</span></span>
          <strong>{{ summary?.total | number }}</strong><span class="card-caption">in selected time range</span>
        </button>
        <button class="summary-card card-mint" [class.selected]="status === 'VALID'" (click)="selectStatus('VALID')">
          <span class="summary-top"><span class="summary-label">VALIDATED</span><span class="card-icon">✓</span></span>
          <strong>{{ summary?.valid | number }}</strong><span class="card-caption"><b>{{ validPercent }}%</b> passing validation</span>
        </button>
        <button class="summary-card card-coral" [class.selected]="status === 'INVALID'" (click)="selectStatus('INVALID')">
          <span class="summary-top"><span class="summary-label">NEEDS ATTENTION</span><span class="card-icon">!</span></span>
          <strong>{{ summary?.invalid | number }}</strong><span class="card-caption">invalid trades to review</span>
        </button>
        <button class="summary-card card-blue" [class.selected]="source === 'KAFKA'" (click)="selectSource('KAFKA')">
          <span class="summary-top"><span class="summary-label">KAFKA LIVE</span><span class="card-icon">⌁</span></span>
          <strong>{{ summary?.kafka | number }}</strong><span class="card-caption"><i class="stream-dot"></i> intraday executions</span>
        </button>
        <button class="summary-card card-amber" [class.selected]="source === 'FILE'" (click)="selectSource('FILE')">
          <span class="summary-top"><span class="summary-label">EOD FILE</span><span class="card-icon">▤</span></span>
          <strong>{{ summary?.file | number }}</strong><span class="card-caption">end-of-day positions</span>
        </button>
      </section>

      <p *ngIf="error" class="notice error" role="alert">{{ error }}</p>
      <p *ngIf="notice" class="notice success" role="status">{{ notice }}</p>

      <section class="ledger">
        <div class="ledger-heading">
          <div>
            <div class="eyebrow"><span class="eyebrow-line"></span> THE TAPE <span class="eyebrow-divider">·</span>
              <span class="updated-label">{{ loading ? 'UPDATING…' : 'UPDATED ' + (now | date:'HH:mm:ss':'+0530') }}</span>
            </div>
            <h1>Trade Ledger <span class="total-pill">{{ page.totalElements | number }} trades</span></h1>
          </div>
          <div class="ledger-actions">
            <label class="upload-button" [class.uploading]="uploading">
              <span>▤ Upload file</span><input type="file" accept=".csv,.txt,text/csv,text/plain" (change)="upload($event)" [disabled]="uploading">
            </label>
            <button class="action-button kafka-button" (click)="kafkaComposerOpen = !kafkaComposerOpen">
              <span>⌁ Send Kafka test</span>
            </button>
            <button class="action-button export-button" (click)="exportReport()" [disabled]="!trades.length" title="Export the current filtered page">⇩ <span>Export report</span></button>
          </div>
        </div>

        <form *ngIf="kafkaComposerOpen" class="kafka-composer" #kafkaForm="ngForm" (ngSubmit)="publishKafkaTrade()">
          <div class="composer-title"><span class="composer-icon">⌁</span><div><strong>Send an intraday Kafka event</strong><small>Published to trades.intraday and processed by the normal validation pipeline.</small></div></div>
          <label>BOOKING SYSTEM<input name="bookingSystem" [(ngModel)]="kafkaDraft.bookingSystem" required maxlength="100"></label>
          <label>TRADE ID<input name="tradeId" [(ngModel)]="kafkaDraft.tradeId" required type="number" min="1" step="1"></label>
          <label>INSTRUMENT<input name="instrumentId" [(ngModel)]="kafkaDraft.instrumentId" required maxlength="100"></label>
          <label>QUANTITY<input name="quantity" [(ngModel)]="kafkaDraft.quantity" required type="number" step="1"></label>
          <label>PRICE<input name="tradePrice" [(ngModel)]="kafkaDraft.tradePrice" required type="number" step="0.0001"></label>
          <label>TRADE TIME<input name="tradeTimestamp" [(ngModel)]="kafkaDraft.tradeTimestamp" required type="datetime-local"></label>
          <button type="submit" class="publish-button" [disabled]="!kafkaForm.valid || sendingKafka">{{ sendingKafka ? 'Sending…' : 'Publish event' }}</button>
          <button type="button" class="cancel-button" (click)="kafkaComposerOpen = false" [disabled]="sendingKafka">Cancel</button>
          <p>Non-positive quantity or price is allowed here to test invalid-trade handling.</p>
        </form>

        <div class="toolbar">
          <div class="range-control">
            <label for="range-select">TIME PERIOD</label>
            <select id="range-select" [ngModel]="range" (ngModelChange)="setRange($event)">
              <option value="ALL">All history</option>
              <option value="TODAY">Today</option>
              <option value="WEEK">This week</option>
              <option value="LAST_7D">Last 7 days</option>
              <option value="MONTH">This month</option>
              <option value="LAST_MONTH">Last month</option>
              <option value="YEAR">This year</option>
              <option value="LAST_YEAR">Last year</option>
              <option value="CUSTOM">Custom range…</option>
            </select>
          </div>
          <div class="search-box">
            <span class="search-icon">⌕</span>
            <input #searchField [ngModel]="search" (ngModelChange)="searchChanged($event)"
                   placeholder="Search trade ID, instrument, booking system…" aria-label="Search all trades" maxlength="128">
            <button *ngIf="search" class="clear-search" aria-label="Clear search" (click)="searchChanged('')">×</button>
            <kbd *ngIf="!search">⌘ K</kbd>
          </div>
          <div class="show-label">SHOW:</div>
          <div class="status-filters">
            <button class="filter-chip" [class.active]="!status" (click)="clearStatusFilter()">All <b>({{ summary?.total | number }})</b></button>
            <button class="filter-chip valid-filter" [class.active]="status === 'VALID'" (click)="selectStatus('VALID')"><i>✓</i> Valid <b>({{ summary?.valid | number }})</b></button>
            <button class="filter-chip invalid-filter" [class.active]="status === 'INVALID'" (click)="selectStatus('INVALID')"><i>!</i> Invalid <b>({{ summary?.invalid | number }})</b></button>
          </div>
          <div class="source-filters">
            <button class="filter-chip" [class.active]="source === 'FILE'" (click)="selectSource('FILE')">EOD File</button>
            <button class="filter-chip" [class.active]="source === 'KAFKA'" (click)="selectSource('KAFKA')">Kafka Live</button>
          </div>
        </div>

        <div *ngIf="range === 'CUSTOM'" class="custom-range">
          <label>From <input type="datetime-local" [(ngModel)]="from" (change)="applyCustomRange()"></label>
          <label>To <input type="datetime-local" [(ngModel)]="to" (change)="applyCustomRange()"></label>
          <button (click)="setRange('ALL')">Clear dates</button>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th><button (click)="sort('BOOKING_SYSTEM')">BOOKING SYSTEM <span>{{ sortIndicator('BOOKING_SYSTEM') }}</span></button></th>
                <th><button (click)="sort('TRADE_ID')">TRADE ID <span>{{ sortIndicator('TRADE_ID') }}</span></button></th>
                <th><button (click)="sort('INSTRUMENT_ID')">INSTRUMENT <span>{{ sortIndicator('INSTRUMENT_ID') }}</span></button></th>
                <th class="right"><button (click)="sort('QUANTITY')">QUANTITY <span>{{ sortIndicator('QUANTITY') }}</span></button></th>
                <th class="right"><button (click)="sort('TRADE_PRICE')">TRADE PRICE <span>{{ sortIndicator('TRADE_PRICE') }}</span></button></th>
                <th><button (click)="sort('TRADE_TIMESTAMP')">TRADE TIME <span>{{ sortIndicator('TRADE_TIMESTAMP') }}</span></button></th>
                <th><button (click)="sort('STATUS')">STATUS <span>{{ sortIndicator('STATUS') }}</span></button></th>
                <th><button (click)="sort('VALIDATION_REASON')">VALIDATION RULE DIAGNOSTICS <span>{{ sortIndicator('VALIDATION_REASON') }}</span></button></th>
                <th><button (click)="sort('SOURCE')">SOURCE <span>{{ sortIndicator('SOURCE') }}</span></button></th>
                <th><button (click)="sort('RECEIVED_AT')">INGESTION TIME <span>{{ sortIndicator('RECEIVED_AT') }}</span></button></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let trade of trades; trackBy: trackTrade" [class.invalid-row]="trade.status === 'INVALID'">
                <td class="system-name">{{ trade.bookingSystem }}</td>
                <td class="id-cell">#{{ trade.tradeId }}</td>
                <td><span class="instrument">{{ trade.instrumentId }}</span></td>
                <td class="right quantity-cell" [class.negative]="trade.quantity < 0" [class.zero]="trade.quantity === 0">{{ trade.quantity > 0 ? '+' : '' }}{{ trade.quantity | number }}</td>
                <td class="right price-cell" [class.invalid-value]="trade.tradePrice <= 0">{{ trade.tradePrice | number:'1.2-4' }}</td>
                <td class="time-cell">{{ trade.tradeTimestamp | date:'dd MMM yy · HH:mm:ss':'+0530' }}</td>
                <td><span class="validation-badge" [class.invalid]="trade.status === 'INVALID'"><i>{{ trade.status === 'VALID' ? '✓' : '!' }}</i>{{ trade.status }}</span></td>
                <td class="diagnostic">{{ trade.validationReason || '—' }}</td>
                <td><span class="source-badge" [class.badge-file]="trade.source === 'FILE'"><i></i>{{ trade.source === 'FILE' ? 'EOD FILE' : 'LIVE' }}</span></td>
                <td class="time-cell">{{ trade.receivedAt | date:'dd MMM yy · HH:mm:ss':'+0530' }}</td>
              </tr>
              <tr *ngIf="trades.length === 0">
                <td colspan="10" class="empty-state"><span>{{ loading ? '◌' : '⌕' }}</span><strong>{{ loading ? 'Loading trades…' : 'No trades match your filters' }}</strong><small>{{ search ? 'Try another search or clear the filters.' : 'Choose another reporting period or clear filters.' }}</small>
                  <button *ngIf="!loading" (click)="clearFilters()">Clear all filters</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer class="pagination">
          <span>Showing <b>{{ trades.length }}</b> of <b>{{ page.totalElements | number }}</b> trades</span>
          <div><button (click)="changePage(-1)" [disabled]="page.page === 0 || loading">← Back</button><span>{{ page.page + 1 }} <i>/</i> {{ pageCount }}</span><button (click)="changePage(1)" [disabled]="page.page + 1 >= pageCount || loading">More →</button></div>
        </footer>
      </section>
      <footer class="page-footer"><span>TRADEFLOW <i>•</i> TRADE CAPTURE & REPORTING</span><span>{{ now | date:'d MMM yyyy':'+0530' }}</span></footer>
    </main>
  `
})
export class AppComponent implements OnInit, OnDestroy {
  @ViewChild('searchField') private searchField?: ElementRef<HTMLInputElement>;

  private readonly api = inject(TradeApiService);
  private readonly searchTerms = new Subject<string>();
  private searchSubscription?: Subscription;
  private polling?: Subscription;

  theme: Theme = 'dark';
  connected = false;
  loading = false;
  uploading = false;
  sendingKafka = false;
  kafkaComposerOpen = false;
  kafkaDraft: KafkaTradePayload = {
    bookingSystem: 'MUREX',
    tradeId: Math.floor(Date.now() / 1000),
    instrumentId: 'IBM',
    quantity: 25,
    tradePrice: 184.25,
    tradeTimestamp: this.toLocalInput(new Date())
  };
  now = new Date();
  from = '';
  to = '';
  range = 'ALL';
  status = '';
  source = '';
  search = '';
  sortBy: TradeSortField = 'TRADE_TIMESTAMP';
  sortDirection: 'ASC' | 'DESC' = 'DESC';
  summary: Summary | null = null;
  trades: Trade[] = [];
  page = { content: [] as Trade[], totalElements: 0, page: 0, size: 50 };
  error = '';
  notice = '';

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('tradeflow-theme');
    this.theme = savedTheme === 'light' ? 'light' : 'dark';
    this.applyTheme();
    this.searchSubscription = this.searchTerms.pipe(
      debounceTime(250),
      distinctUntilChanged()
    ).subscribe(() => {
      this.page.page = 0;
      this.load();
    });
    this.load();
    this.polling = timer(10_000, 10_000).subscribe(() => this.load());
  }

  ngOnDestroy(): void {
    this.polling?.unsubscribe();
    this.searchSubscription?.unsubscribe();
  }

  get pageCount(): number {
    return Math.max(1, Math.ceil(this.page.totalElements / this.page.size));
  }

  get validPercent(): number {
    return this.summary?.total ? Math.round((this.summary.valid / this.summary.total) * 100) : 0;
  }

  toggleTheme(): void {
    this.theme = this.theme === 'dark' ? 'light' : 'dark';
    localStorage.setItem('tradeflow-theme', this.theme);
    this.applyTheme();
  }

  setRange(range: string): void {
    this.range = range;
    this.from = '';
    this.to = '';
    const now = new Date();
    const start = new Date(now);
    const end = new Date(now);

    switch (range) {
      case 'TODAY':
        start.setHours(0, 0, 0, 0);
        this.setDateRange(start, end);
        break;
      case 'WEEK':
        start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
        start.setHours(0, 0, 0, 0);
        this.setDateRange(start, end);
        break;
      case 'LAST_7D':
        start.setDate(start.getDate() - 7);
        this.setDateRange(start, end);
        break;
      case 'MONTH':
        start.setDate(1);
        start.setHours(0, 0, 0, 0);
        this.setDateRange(start, end);
        break;
      case 'LAST_MONTH':
        start.setDate(1);
        start.setMonth(start.getMonth() - 1);
        start.setHours(0, 0, 0, 0);
        end.setDate(0);
        end.setHours(23, 59, 59, 0);
        this.setDateRange(start, end);
        break;
      case 'YEAR':
        start.setMonth(0, 1);
        start.setHours(0, 0, 0, 0);
        this.setDateRange(start, end);
        break;
      case 'LAST_YEAR':
        start.setFullYear(start.getFullYear() - 1, 0, 1);
        start.setHours(0, 0, 0, 0);
        end.setMonth(0, 0);
        end.setHours(23, 59, 59, 0);
        this.setDateRange(start, end);
        break;
      case 'CUSTOM':
        return;
      default:
        this.page.page = 0;
        this.load();
        return;
    }
    this.page.page = 0;
    this.load();
  }

  applyCustomRange(): void {
    if (this.from && this.to && new Date(this.from) > new Date(this.to)) {
      this.error = 'The start date must be earlier than the end date.';
      return;
    }
    this.page.page = 0;
    this.load();
  }

  searchChanged(value: string): void {
    this.search = value;
    this.searchTerms.next(value);
  }

  selectStatus(status: string): void {
    this.status = this.status === status ? '' : status;
    this.page.page = 0;
    this.load();
  }

  clearStatusFilter(): void {
    this.status = '';
    this.page.page = 0;
    this.load();
  }

  showAllTrades(): void {
    this.status = '';
    this.source = '';
    this.page.page = 0;
    this.load();
  }

  selectSource(source: string): void {
    this.source = this.source === source ? '' : source;
    this.page.page = 0;
    this.load();
  }

  sort(field: TradeSortField): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.sortBy = field;
      this.sortDirection = field === 'TRADE_TIMESTAMP' || field === 'RECEIVED_AT' || field === 'TRADE_ID'
        ? 'DESC'
        : 'ASC';
    }
    this.page.page = 0;
    this.load();
  }

  sortIndicator(field: string): string {
    return this.sortBy !== field ? '↕' : this.sortDirection === 'ASC' ? '↑' : '↓';
  }

  changePage(delta: number): void {
    this.page.page += delta;
    this.load();
  }

  clearFilters(): void {
    this.status = '';
    this.source = '';
    this.searchChanged('');
    this.setRange('ALL');
  }

  upload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.error = '';
    this.notice = '';
    this.uploading = true;
    this.api.upload(file).subscribe({
      next: result => {
        this.uploading = false;
        this.notice = `${result.accepted.toLocaleString()} file records processed. Invalid business records are kept for review.`;
        input.value = '';
        this.load();
      },
      error: error => {
        this.uploading = false;
        this.handleError(error);
      }
    });
  }

  publishKafkaTrade(): void {
    this.error = '';
    this.notice = '';
    this.sendingKafka = true;
    const event: KafkaTradePayload = {
      ...this.kafkaDraft,
      tradeTimestamp: new Date(this.kafkaDraft.tradeTimestamp).toISOString()
    };
    this.api.publishKafkaTrade(event).subscribe({
      next: result => {
        this.sendingKafka = false;
        this.kafkaComposerOpen = false;
        this.notice = `Trade ${result.tradeId} acknowledged by Kafka (${result.topic}); the consumer is processing it.`;
        this.kafkaDraft = {
          ...this.kafkaDraft,
          tradeId: Math.max(Math.floor(Date.now() / 1000), this.kafkaDraft.tradeId + 1),
          tradeTimestamp: this.toLocalInput(new Date())
        };
        this.load();
      },
      error: error => {
        this.sendingKafka = false;
        this.handleError(error);
      }
    });
  }

  exportReport(): void {
    const columns = ['BookingSystem', 'TradeID', 'InstrumentID', 'Quantity', 'TradePrice',
      'TradeTimestamp', 'Status', 'ValidationReason', 'Source', 'IngestionTime'];
    const rows = this.trades.map(trade => [
      trade.bookingSystem, trade.tradeId, trade.instrumentId, trade.quantity, trade.tradePrice,
      this.toIstIso(trade.tradeTimestamp), trade.status, trade.validationReason ?? '',
      trade.source, this.toIstIso(trade.receivedAt)
    ]);
    const csv = [columns, ...rows].map(row => row.map(value => this.csvCell(value)).join(',')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `tradeflow-report-${this.toIstIso(new Date().toISOString()).slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  trackTrade(_index: number, trade: Trade): string {
    return trade.id;
  }

  @HostListener('window:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.searchField?.nativeElement.focus();
    }
  }

  load(): void {
    if (this.loading) return;
    this.loading = true;
    this.error = '';
    const query: Query = {
      status: this.status, source: this.source, from: this.from, to: this.to,
      page: this.page.page, size: this.page.size, search: this.search.trim(),
      sortBy: this.sortBy, sortDirection: this.sortDirection
    };
    forkJoin({
      summary: this.api.getSummary(this.from, this.to),
      page: this.api.getTrades(query)
    }).pipe(tap(result => {
      this.summary = result.summary;
      this.page = result.page;
      this.trades = result.page.content;
      this.connected = true;
      this.loading = false;
      this.now = new Date();
    })).subscribe({
      error: error => {
        this.loading = false;
        this.connected = false;
        this.handleError(error);
      }
    });
  }

  private setDateRange(from: Date, to: Date): void {
    this.from = this.toLocalInput(from);
    this.to = this.toLocalInput(to);
  }

  private toLocalInput(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }

  private applyTheme(): void {
    document.documentElement.dataset['theme'] = this.theme;
  }

  private csvCell(value: string | number): string {
    let text = String(value ?? '');
    if (/^[=+\-@]/.test(text)) text = `'${text}`;
    return `"${text.replaceAll('"', '""')}"`;
  }

  private toIstIso(value: string): string {
    return new Date(value).toLocaleString('sv-SE', {
      timeZone: 'Asia/Kolkata',
      hour12: false
    }).replace(' ', 'T') + '+05:30';
  }

  private handleError(error: unknown): void {
    this.error = error instanceof HttpErrorResponse
      ? error.error?.detail ?? `The request failed (${error.status || 'network error'}).`
      : 'An unexpected request error occurred.';
  }
}
