import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CartDto } from '../models/cart.model';
import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;
  let httpMock: HttpTestingController;

  const cart: CartDto = {
    id: 'cart-1',
    userId: 'user-1',
    items: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CartService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('posts the product and quantity to /cart/items', () => {
    service.addItem('product-1', 2).subscribe((result) => expect(result).toEqual(cart));

    const request = httpMock.expectOne('/cart/items');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ productId: 'product-1', quantity: 2 });
    request.flush(cart);
  });

  it('defaults the quantity to one', () => {
    service.addItem('product-1').subscribe();

    const request = httpMock.expectOne('/cart/items');
    expect(request.request.body).toEqual({ productId: 'product-1', quantity: 1 });
    request.flush(cart);
  });
});
