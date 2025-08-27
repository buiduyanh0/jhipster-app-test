import dayjs from 'dayjs/esm';

export interface IMember {
  id: number;
  name?: string | null;
  email?: string | null;
  joinDate?: dayjs.Dayjs | null;
}

export type NewMember = Omit<IMember, 'id'> & { id: null };
