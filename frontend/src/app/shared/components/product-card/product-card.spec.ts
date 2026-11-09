import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductListDto } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';
import { env } from '../../../../environments/environment';
import { MOCK_DEFAULT_STOCK } from '../../../features/cart/cart.mock';
import { ProductCard } from './product-card';

describe('ProductCard', () => {
  let fixture: ComponentFixture<ProductCard>;

  const product: ProductListDto = {
    id: 'p1',
    name: 'Desk lamp',
    price: 45,
    image: 'img-1',
    category: 'HOME_LIVING',
    createdAt: '2026-10-01T10:00:00Z',
  };

  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: CartService,
          useValue: jasmine.createSpyObj<CartService>('CartService', ['addItem']),
        },
      ],
    });
    fixture = TestBed.createComponent(ProductCard);
    fixture.componentRef.setInput('product', product);
  });

  it('shows the product details', () => {
    fixture.detectChanges();

    expect(el().querySelector('.product-name')?.textContent).toContain('Desk lamp');
    expect(el().querySelector('.card-category')?.textContent).toContain('Home Living');
    expect(el().querySelector('.product-price')?.textContent).toContain('$45.00');
    expect(el().querySelector('.product-image')?.getAttribute('src')).toBe(
      `${env.mediaUrl}/img-1`,
    );
  });

  it('shows a placeholder without an image', () => {
    fixture.componentRef.setInput('product', { ...product, image: undefined });
    fixture.detectChanges();

    expect(el().querySelector('.image-placeholder')).not.toBeNull();
    expect(el().querySelector('.product-image')).toBeNull();
  });

  it('shows the badge when the product has one', () => {
    fixture.componentRef.setInput('product', { ...product, badge: 'New' });
    fixture.detectChanges();

    expect(el().querySelector('.card-badge')?.textContent).toContain('New');
  });

  it('has no add-to-cart button unless it is asked for', () => {
    fixture.detectChanges();
    expect(el().querySelector('app-add-to-cart-button')).toBeNull();
  });

  it('shows an icon-only add-to-cart button on the shop grid', () => {
    fixture.componentRef.setInput('showAddToCart', true);
    fixture.detectChanges();

    const button = el().querySelector('app-add-to-cart-button button');
    expect(button?.getAttribute('aria-label')).toBe('Add Desk lamp to cart');
  });

  it('turns the product into a cart line', () => {
    expect(fixture.componentInstance.cartProduct()).toEqual({
      id: 'p1',
      name: 'Desk lamp',
      category: 'Home Living',
      price: 45,
      stock: MOCK_DEFAULT_STOCK,
      image: `${env.mediaUrl}/img-1`,
    });
  });

  it('uses the placeholder image for a cart line without an image', () => {
    fixture.componentRef.setInput('product', { ...product, image: undefined });

    expect(fixture.componentInstance.cartProduct().image).toBe('/assets/placeholder.png');
  });
});
