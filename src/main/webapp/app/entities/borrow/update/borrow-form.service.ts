import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IBorrow, NewBorrow } from '../borrow.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IBorrow for edit and NewBorrowFormGroupInput for create.
 */
type BorrowFormGroupInput = IBorrow | PartialWithRequiredKeyOf<NewBorrow>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IBorrow | NewBorrow> = Omit<T, 'borrowDate' | 'returnDate'> & {
  borrowDate?: string | null;
  returnDate?: string | null;
};

type BorrowFormRawValue = FormValueOf<IBorrow>;

type NewBorrowFormRawValue = FormValueOf<NewBorrow>;

type BorrowFormDefaults = Pick<NewBorrow, 'id' | 'borrowDate' | 'returnDate'>;

type BorrowFormGroupContent = {
  id: FormControl<BorrowFormRawValue['id'] | NewBorrow['id']>;
  borrowDate: FormControl<BorrowFormRawValue['borrowDate']>;
  returnDate: FormControl<BorrowFormRawValue['returnDate']>;
  member: FormControl<BorrowFormRawValue['member']>;
  book: FormControl<BorrowFormRawValue['book']>;
};

export type BorrowFormGroup = FormGroup<BorrowFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class BorrowFormService {
  createBorrowFormGroup(borrow: BorrowFormGroupInput = { id: null }): BorrowFormGroup {
    const borrowRawValue = this.convertBorrowToBorrowRawValue({
      ...this.getFormDefaults(),
      ...borrow,
    });
    return new FormGroup<BorrowFormGroupContent>({
      id: new FormControl(
        { value: borrowRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      borrowDate: new FormControl(borrowRawValue.borrowDate, {
        validators: [Validators.required],
      }),
      returnDate: new FormControl(borrowRawValue.returnDate),
      member: new FormControl(borrowRawValue.member),
      book: new FormControl(borrowRawValue.book),
    });
  }

  getBorrow(form: BorrowFormGroup): IBorrow | NewBorrow {
    return this.convertBorrowRawValueToBorrow(form.getRawValue() as BorrowFormRawValue | NewBorrowFormRawValue);
  }

  resetForm(form: BorrowFormGroup, borrow: BorrowFormGroupInput): void {
    const borrowRawValue = this.convertBorrowToBorrowRawValue({ ...this.getFormDefaults(), ...borrow });
    form.reset(
      {
        ...borrowRawValue,
        id: { value: borrowRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): BorrowFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      borrowDate: currentTime,
      returnDate: currentTime,
    };
  }

  private convertBorrowRawValueToBorrow(rawBorrow: BorrowFormRawValue | NewBorrowFormRawValue): IBorrow | NewBorrow {
    return {
      ...rawBorrow,
      borrowDate: dayjs(rawBorrow.borrowDate, DATE_TIME_FORMAT),
      returnDate: dayjs(rawBorrow.returnDate, DATE_TIME_FORMAT),
    };
  }

  private convertBorrowToBorrowRawValue(
    borrow: IBorrow | (Partial<NewBorrow> & BorrowFormDefaults),
  ): BorrowFormRawValue | PartialWithRequiredKeyOf<NewBorrowFormRawValue> {
    return {
      ...borrow,
      borrowDate: borrow.borrowDate ? borrow.borrowDate.format(DATE_TIME_FORMAT) : undefined,
      returnDate: borrow.returnDate ? borrow.returnDate.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
