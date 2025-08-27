import dayjs from 'dayjs/esm';

import { IBorrow, NewBorrow } from './borrow.model';

export const sampleWithRequiredData: IBorrow = {
  id: 7652,
  borrowDate: dayjs('2025-08-21T08:26'),
};

export const sampleWithPartialData: IBorrow = {
  id: 6459,
  borrowDate: dayjs('2025-08-21T06:17'),
};

export const sampleWithFullData: IBorrow = {
  id: 16522,
  borrowDate: dayjs('2025-08-21T03:39'),
  returnDate: dayjs('2025-08-21T06:12'),
};

export const sampleWithNewData: NewBorrow = {
  borrowDate: dayjs('2025-08-21T07:16'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
