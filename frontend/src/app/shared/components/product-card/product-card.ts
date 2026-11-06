import { Component, input } from '@angular/core';
import { Image as ImageIcon, LucideAngularModule } from 'lucide-angular';
import { ProductListDto } from '../../../core/models/product.model';
import { formatCategory } from '../../utils/category.utils';
import { env } from '../../../../environments/environment';

/** Presentational product card used by the shop grid and "More from …" on the detail page. */
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './product-card.html',
  styleUrls: ['./product-card.scss'],
})
export class ProductCard {
  readonly ImageIcon = ImageIcon;
  readonly formatCategory = formatCategory;

  product = input.required<ProductListDto>();

  imageUrl(id?: string): string {
    return `${env.mediaUrl}/${id}`;
  }
}
