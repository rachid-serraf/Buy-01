import { Component, computed, input } from '@angular/core';
import { Image as ImageIcon, LucideAngularModule } from 'lucide-angular';
import { ProductListDto } from '../../../core/models/product.model';
import { formatCategory } from '../../utils/category.utils';
import { env } from '../../../../environments/environment';
import { AddToCartButton } from '../../../cart/add-to-cart-button/add-to-cart-button';
import { CartProduct } from '../../../cart/cart-ui.state';
import { MOCK_DEFAULT_STOCK } from '../../../cart/cart.mock';

/** Presentational product card used by the shop grid and "More from …" on the detail page. */
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [AddToCartButton, LucideAngularModule],
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.scss'],
})
export class ProductCard {
  readonly ImageIcon = ImageIcon;
  readonly formatCategory = formatCategory;

  product = input.required<ProductListDto>();
  showAddToCart = input(false);

  cartProduct = computed<CartProduct>(() => {
    const p = this.product();
    return {
      id: p.id,
      name: p.name,
      category: formatCategory(p.category),
      price: p.price,
      stock: MOCK_DEFAULT_STOCK,
      image: p.image ? this.imageUrl(p.image) : '/assets/placeholder.png',
    };
  });

  imageUrl(id?: string): string {
    return `${env.mediaUrl}/${id}`;
  }
}
