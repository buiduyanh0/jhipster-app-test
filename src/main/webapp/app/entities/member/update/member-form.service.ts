import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IMember, NewMember } from '../member.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IMember for edit and NewMemberFormGroupInput for create.
 */
type MemberFormGroupInput = IMember | PartialWithRequiredKeyOf<NewMember>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IMember | NewMember> = Omit<T, 'joinDate'> & {
  joinDate?: string | null;
};

type MemberFormRawValue = FormValueOf<IMember>;

type NewMemberFormRawValue = FormValueOf<NewMember>;

type MemberFormDefaults = Pick<NewMember, 'id' | 'joinDate'>;

type MemberFormGroupContent = {
  id: FormControl<MemberFormRawValue['id'] | NewMember['id']>;
  name: FormControl<MemberFormRawValue['name']>;
  email: FormControl<MemberFormRawValue['email']>;
  joinDate: FormControl<MemberFormRawValue['joinDate']>;
};

export type MemberFormGroup = FormGroup<MemberFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class MemberFormService {
  createMemberFormGroup(member: MemberFormGroupInput = { id: null }): MemberFormGroup {
    const memberRawValue = this.convertMemberToMemberRawValue({
      ...this.getFormDefaults(),
      ...member,
    });
    return new FormGroup<MemberFormGroupContent>({
      id: new FormControl(
        { value: memberRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(memberRawValue.name, {
        validators: [Validators.required],
      }),
      email: new FormControl(memberRawValue.email, {
        validators: [Validators.required],
      }),
      joinDate: new FormControl(memberRawValue.joinDate, {
        validators: [Validators.required],
      }),
    });
  }

  getMember(form: MemberFormGroup): IMember | NewMember {
    return this.convertMemberRawValueToMember(form.getRawValue() as MemberFormRawValue | NewMemberFormRawValue);
  }

  resetForm(form: MemberFormGroup, member: MemberFormGroupInput): void {
    const memberRawValue = this.convertMemberToMemberRawValue({ ...this.getFormDefaults(), ...member });
    form.reset(
      {
        ...memberRawValue,
        id: { value: memberRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): MemberFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      joinDate: currentTime,
    };
  }

  private convertMemberRawValueToMember(rawMember: MemberFormRawValue | NewMemberFormRawValue): IMember | NewMember {
    return {
      ...rawMember,
      joinDate: dayjs(rawMember.joinDate, DATE_TIME_FORMAT),
    };
  }

  private convertMemberToMemberRawValue(
    member: IMember | (Partial<NewMember> & MemberFormDefaults),
  ): MemberFormRawValue | PartialWithRequiredKeyOf<NewMemberFormRawValue> {
    return {
      ...member,
      joinDate: member.joinDate ? member.joinDate.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
