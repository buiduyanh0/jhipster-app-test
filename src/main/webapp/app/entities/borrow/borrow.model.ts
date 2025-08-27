import dayjs from 'dayjs/esm';
import { IMember } from 'app/entities/member/member.model';
import { IBook } from 'app/entities/book/book.model';

export interface IBorrow {
  id: number;
  borrowDate?: dayjs.Dayjs | null;
  returnDate?: dayjs.Dayjs | null;
  member?: IMember | null;
  book?: IBook | null;
}

export type NewBorrow = Omit<IBorrow, 'id'> & { id: null };
