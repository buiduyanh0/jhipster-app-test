import dayjs from 'dayjs/esm';

import { IMember, NewMember } from './member.model';

export const sampleWithRequiredData: IMember = {
  id: 26797,
  name: 'huzzah',
  email: 'DuyNgon67@yahoo.com',
  joinDate: dayjs('2025-08-21T23:30'),
};

export const sampleWithPartialData: IMember = {
  id: 30382,
  name: 'whether kindly',
  email: 'LuongThien_Nguyen@yahoo.com',
  joinDate: dayjs('2025-08-21T11:21'),
};

export const sampleWithFullData: IMember = {
  id: 30531,
  name: 'triangular first',
  email: 'NgocMai32@yahoo.com',
  joinDate: dayjs('2025-08-22T01:05'),
};

export const sampleWithNewData: NewMember = {
  name: 'mystify',
  email: 'AnDi.Ho@hotmail.com',
  joinDate: dayjs('2025-08-21T15:05'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
