import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { LucideAngularModule, MapPin } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import { OrderAddress } from '../../../../core/models/order.model';

export interface DeliveryAddressFormData {
  address?: OrderAddress | null;
}

@Component({
  selector: 'app-delivery-address-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    LucideAngularModule,
  ],
  templateUrl: './delivery-address-form.html',
  styleUrl: './delivery-address-form.scss',
})
export class DeliveryAddressFormDialog {
  readonly MapPin = MapPin;

  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<DeliveryAddressFormDialog, OrderAddress>);
  private readonly data = inject<DeliveryAddressFormData | null>(MAT_DIALOG_DATA, {
    optional: true,
  });

  readonly form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    line1: ['', Validators.required],
    line2: [''],
    city: ['', Validators.required],
    state: [''],
    postalCode: ['', Validators.required],
    country: ['', Validators.required],
  });

  constructor() {
    const initial = this.data?.address;
    if (!initial) return;

    this.form.patchValue({
      fullName: initial.fullName ?? '',
      phone: initial.phone ?? '',
      line1: initial.line1 ?? '',
      line2: initial.line2 ?? '',
      city: initial.city ?? '',
      state: initial.state ?? '',
      postalCode: initial.postalCode ?? '',
      country: initial.country ?? '',
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const address: OrderAddress = {
      fullName: value.fullName.trim(),
      phone: value.phone.trim(),
      line1: value.line1.trim(),
      city: value.city.trim(),
      postalCode: value.postalCode.trim(),
      country: value.country.trim(),
    };

    const line2 = value.line2.trim();
    const state = value.state.trim();
    if (line2) address.line2 = line2;
    if (state) address.state = state;

    this.dialogRef.close(address);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}

export function openDeliveryAddressDialog(
  dialog: MatDialog,
  address?: OrderAddress | null,
): Promise<OrderAddress | null> {
  const ref = dialog.open<
    DeliveryAddressFormDialog,
    DeliveryAddressFormData,
    OrderAddress
  >(DeliveryAddressFormDialog, {
    width: '620px',
    maxWidth: '95vw',
    autoFocus: 'first-tabbable',
    data: { address: address ?? null },
  });

  return firstValueFrom(ref.afterClosed()).then((result) => result ?? null);
}
