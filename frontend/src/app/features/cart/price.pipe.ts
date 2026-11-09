import { Pipe, PipeTransform } from '@angular/core';

/** Same output as the product pages: $129.00 */
@Pipe({ name: 'price' })
export class PricePipe implements PipeTransform {
  transform(value: number): string {
    return `$${value.toFixed(2)}`;
  }
}
