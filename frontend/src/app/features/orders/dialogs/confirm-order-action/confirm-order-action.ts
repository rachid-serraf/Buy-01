import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { LucideAngularModule, AlertTriangle, Info } from 'lucide-angular';

export interface ConfirmOrderActionData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
}

@Component({
  selector: 'app-confirm-order-action',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, LucideAngularModule],
  templateUrl: './confirm-order-action.html',
  styleUrl: './confirm-order-action.scss',
})
export class ConfirmOrderActionDialog {
  readonly AlertTriangle = AlertTriangle;
  readonly Info = Info;

  readonly data = inject<ConfirmOrderActionData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmOrderActionDialog, boolean>);

  close(confirmed: boolean): void {
    this.dialogRef.close(confirmed);
  }
}
