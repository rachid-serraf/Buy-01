import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuantityStepper } from './quantity-stepper';

describe('QuantityStepper', () => {
  let fixture: ComponentFixture<QuantityStepper>;
  let emitted: number[];

  const buttons = () =>
    fixture.nativeElement.querySelectorAll('.qty-btn') as NodeListOf<HTMLButtonElement>;
  const minus = () => buttons()[0];
  const plus = () => buttons()[1];
  const tooltip = () => fixture.nativeElement.querySelector('.qty-tooltip') as HTMLElement | null;

  function render(value: number, max: number, disabled = false): void {
    fixture.componentRef.setInput('value', value);
    fixture.componentRef.setInput('max', max);
    fixture.componentRef.setInput('disabled', disabled);
    fixture.detectChanges();
  }

  beforeEach(() => {
    fixture = TestBed.createComponent(QuantityStepper);
    fixture.componentRef.setInput('itemName', 'Mug');
    emitted = [];
    fixture.componentInstance.valueChange.subscribe((value) => emitted.push(value));
  });

  it('shows the value and names the item in its labels', () => {
    render(2, 5);

    expect(fixture.nativeElement.querySelector('.qty-value').textContent.trim()).toBe('2');
    expect(minus().getAttribute('aria-label')).toBe('Decrease quantity of Mug');
    expect(plus().getAttribute('aria-label')).toBe('Increase quantity of Mug');
  });

  it('asks for one more or one less', () => {
    render(2, 5);

    plus().click();
    minus().click();

    expect(emitted).toEqual([3, 1]);
  });

  it('disables minus at a quantity of one', () => {
    render(1, 5);
    expect(minus().disabled).toBeTrue();
  });

  it('blocks plus at max stock but keeps it focusable for the tooltip', () => {
    render(5, 5);

    plus().click();

    expect(emitted).toEqual([]);
    expect(plus().disabled).toBeFalse();
    expect(plus().getAttribute('aria-disabled')).toBe('true');
    expect(tooltip()?.textContent?.trim()).toBe('Only 5 in stock');
    expect(plus().getAttribute('aria-describedby')).toBe(tooltip()?.id ?? null);
  });

  it('has no tooltip below max stock', () => {
    render(4, 5);

    expect(tooltip()).toBeNull();
    expect(plus().hasAttribute('aria-disabled')).toBeFalse();
  });

  it('disables both buttons without a tooltip when the stepper is disabled', () => {
    render(1, 0, true);

    expect(minus().disabled).toBeTrue();
    expect(plus().disabled).toBeTrue();
    expect(tooltip()).toBeNull();
  });

  it('gives each stepper its own tooltip id', () => {
    const other = TestBed.createComponent(QuantityStepper);
    expect(other.componentInstance.tooltipId).not.toBe(fixture.componentInstance.tooltipId);
  });

  it('applies the compact style', () => {
    fixture.componentRef.setInput('compact', true);
    render(2, 5);
    expect(fixture.nativeElement.querySelector('.qty-stepper.compact')).not.toBeNull();
  });
});
