import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { BookService } from '../service/book.service';
import { IBook } from '../book.model';
import { BookFormService } from './book-form.service';

import { BookUpdateComponent } from './book-update.component';

describe('Book Management Update Component', () => {
  let comp: BookUpdateComponent;
  let fixture: ComponentFixture<BookUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let bookFormService: BookFormService;
  let bookService: BookService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BookUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(BookUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(BookUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    bookFormService = TestBed.inject(BookFormService);
    bookService = TestBed.inject(BookService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const book: IBook = { id: 17120 };

      activatedRoute.data = of({ book });
      comp.ngOnInit();

      expect(comp.book).toEqual(book);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBook>>();
      const book = { id: 32624 };
      jest.spyOn(bookFormService, 'getBook').mockReturnValue(book);
      jest.spyOn(bookService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ book });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: book }));
      saveSubject.complete();

      // THEN
      expect(bookFormService.getBook).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(bookService.update).toHaveBeenCalledWith(expect.objectContaining(book));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBook>>();
      const book = { id: 32624 };
      jest.spyOn(bookFormService, 'getBook').mockReturnValue({ id: null });
      jest.spyOn(bookService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ book: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: book }));
      saveSubject.complete();

      // THEN
      expect(bookFormService.getBook).toHaveBeenCalled();
      expect(bookService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBook>>();
      const book = { id: 32624 };
      jest.spyOn(bookService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ book });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(bookService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
