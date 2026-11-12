import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { OrderAddress } from '../../../../core/models/order.model';
import {
  DeliveryAddressFormDialog,
  DeliveryAddressFormData,
} from './delivery-address-form';

describe('DeliveryAddressFormDialog', () => {
  let fixture: ComponentFixture<DeliveryAddressFormDialog>;
  let component: DeliveryAddressFormDialog;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DeliveryAddressFormDialog, OrderAddress>>;

  const address: OrderAddress = {
    fullName: 'Ada Lovelace',
    phone: '+1 555 0100',
    line1: '1 Main St',
    line2: 'Apt 2',
    city: 'Springfield',
    state: 'IL',
    postalCode: '62701',
    country: 'USA',
  };

  async function create(data: DeliveryAddressFormData): Promise<void> {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [DeliveryAddressFormDialog],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeliveryAddressFormDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => TestBed.resetTestingModule());

  it('requires the mandatory address fields', async () => {
    await create({ address: null });

    component.save();

    expect(component.form.invalid).toBeTrue();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('returns a trimmed delivery address', async () => {
    await create({ address: null });

    component.form.setValue({
      fullName: '  Ada Lovelace ',
      phone: ' +1 555 0100 ',
      line1: ' 1 Main St ',
      line2: ' Apt 2 ',
      city: ' Springfield ',
      state: ' IL ',
      postalCode: ' 62701 ',
      country: ' USA ',
    });
    component.save();

    expect(dialogRef.close).toHaveBeenCalledWith(address);
  });

  it('prefills the form with a previously used address', async () => {
    await create({ address });

    expect(component.form.getRawValue()).toEqual({
      fullName: 'Ada Lovelace',
      phone: '+1 555 0100',
      line1: '1 Main St',
      line2: 'Apt 2',
      city: 'Springfield',
      state: 'IL',
      postalCode: '62701',
      country: 'USA',
    });
  });
});
