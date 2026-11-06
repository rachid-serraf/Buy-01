import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartItem } from '../cart.mock';
import { cartDrawerOpen, cartItems } from '../cart-ui.state';
import { CartDrawer } from './cart-drawer';

describe('CartDrawer', () => {
  let fixture: ComponentFixture<CartDrawer>;
  let trigger: HTMLButtonElement;

  const line = (overrides: Partial<CartItem> = {}): CartItem => ({
    id: 'a',
    name: 'Mug',
    category: 'Home',
    price: 38,
    quantity: 1,
    stock: 5,
    image: 'data:,',
    ...overrides,
  });

  const el = () => fixture.nativeElement as HTMLElement;
  const root = document.documentElement;

  async function setOpen(open: boolean): Promise<void> {
    cartDrawerOpen.set(open);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(() => {
    cartItems.set([line(), line({ id: 'b', name: 'Lamp', stock: 0 })]);
    cartDrawerOpen.set(false);

    trigger = document.createElement('button');
    document.body.appendChild(trigger);

    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(CartDrawer);
    fixture.detectChanges();
  });

  afterEach(() => {
    cartDrawerOpen.set(false);
    fixture.destroy();
    trigger.remove();
    root.style.overflow = '';
    root.style.paddingRight = '';
  });

  it('is closed until it is opened', () => {
    expect(el().querySelector('.cart-drawer.open')).toBeNull();
    expect(root.style.overflow).toBe('');
  });

  it('shows the title, lines and subtotal', async () => {
    await setOpen(true);

    expect(el().querySelector('.cart-drawer.open')).not.toBeNull();
    expect(el().querySelector('#cart-drawer-title')?.textContent).toContain('Your cart (2)');
    expect(el().querySelectorAll('app-cart-item').length).toBe(2);
    expect(el().querySelector('.subtotal strong')?.textContent).toContain('$38.00');
  });

  it('shows how far away free delivery is', async () => {
    await setOpen(true);

    expect(el().querySelector('.delivery p')?.textContent).toContain(
      '$12.00 away from free delivery',
    );
    expect(el().querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('76');
  });

  it('celebrates free delivery once it is reached', async () => {
    cartItems.set([line({ price: 60 })]);
    await setOpen(true);

    expect(el().querySelector('.delivery p')?.textContent).toContain(
      "You've unlocked free delivery",
    );
  });

  it('shows the empty state without the footer', async () => {
    cartItems.set([]);
    await setOpen(true);

    expect(el().querySelector('.empty-state h3')?.textContent).toContain('Your cart is empty');
    expect(el().querySelector('.drawer-footer')).toBeNull();
  });

  it('locks page scroll while open and restores it afterwards', async () => {
    await setOpen(true);
    expect(root.style.overflow).toBe('hidden');

    await setOpen(false);
    expect(root.style.overflow).toBe('');
    expect(root.style.paddingRight).toBe('');
  });

  it('moves focus into the drawer and gives it back on close', async () => {
    trigger.focus();

    await setOpen(true);
    expect(document.activeElement?.getAttribute('aria-label')).toBe('Close cart');

    await setOpen(false);
    expect(document.activeElement).toBe(trigger);
  });

  it('closes on Esc', async () => {
    await setOpen(true);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(cartDrawerOpen()).toBeFalse();
  });

  it('closes on an overlay click', async () => {
    await setOpen(true);

    (el().querySelector('.cart-overlay') as HTMLElement).click();

    expect(cartDrawerOpen()).toBeFalse();
  });

  it('closes with the close button', async () => {
    await setOpen(true);

    (el().querySelector('.close-btn') as HTMLButtonElement).click();

    expect(cartDrawerOpen()).toBeFalse();
  });

  it('closes when going to the cart page', async () => {
    await setOpen(true);

    (el().querySelector('.actions a') as HTMLAnchorElement).click();

    expect(cartDrawerOpen()).toBeFalse();
  });

  it('stays open on checkout until the checkout page exists', async () => {
    await setOpen(true);

    (el().querySelector('.actions button') as HTMLButtonElement).click();

    expect(cartDrawerOpen()).toBeTrue();
  });
});
