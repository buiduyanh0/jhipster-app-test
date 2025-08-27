import { IBook, NewBook } from './book.model';

export const sampleWithRequiredData: IBook = {
  id: 3991,
  title: 'blue regal',
  author: 'nocturnal wordy',
};

export const sampleWithPartialData: IBook = {
  id: 26838,
  title: 'whoever gym until',
  author: 'engender throughout',
};

export const sampleWithFullData: IBook = {
  id: 8637,
  title: 'sniff',
  author: 'however quicker',
  publishedYear: 24504,
  price: 3232.09,
  available: true,
};

export const sampleWithNewData: NewBook = {
  title: 'taxicab nor',
  author: 'agreeable boo',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
