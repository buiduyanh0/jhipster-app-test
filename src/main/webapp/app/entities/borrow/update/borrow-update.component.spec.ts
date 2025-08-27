import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IMember } from 'app/entities/member/member.model';
import { MemberService } from 'app/entities/member/service/member.service';
import { IBook } from 'app/entities/book/book.model';
import { BookService } from 'app/entities/book/service/book.service';
import { IBorrow } from '../borrow.model';
import { BorrowService } from '../service/borrow.service';
import { BorrowFormService } from './borrow-form.service';

import { BorrowUpdateComponent } from './borrow-update.component';

describe('Borrow Management Update Component', () => {
  let comp: BorrowUpdateComponent;
  let fixture: ComponentFixture<BorrowUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let borrowFormService: BorrowFormService;
  let borrowService: BorrowService;
  let memberService: MemberService;
  let bookService: BookService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BorrowUpdateComponent],
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
      .overrideTemplate(BorrowUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(BorrowUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    borrowFormService = TestBed.inject(BorrowFormService);
    borrowService = TestBed.inject(BorrowService);
    memberService = TestBed.inject(MemberService);
    bookService = TestBed.inject(BookService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Member query and add missing value', () => {
      const borrow: IBorrow = { id: 23587 };
      const member: IMember = { id: 17514 };
      borrow.member = member;

      const memberCollection: IMember[] = [{ id: 17514 }];
      jest.spyOn(memberService, 'query').mockReturnValue(of(new HttpResponse({ body: memberCollection })));
      const additionalMembers = [member];
      const expectedCollection: IMember[] = [...additionalMembers, ...memberCollection];
      jest.spyOn(memberService, 'addMemberToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ borrow });
      comp.ngOnInit();

      expect(memberService.query).toHaveBeenCalled();
      expect(memberService.addMemberToCollectionIfMissing).toHaveBeenCalledWith(
        memberCollection,
        ...additionalMembers.map(expect.objectContaining),
      );
      expect(comp.membersSharedCollection).toEqual(expectedCollection);
    });

    it('should call Book query and add missing value', () => {
      const borrow: IBorrow = { id: 23587 };
      const book: IBook = { id: 32624 };
      borrow.book = book;

      const bookCollection: IBook[] = [{ id: 32624 }];
      jest.spyOn(bookService, 'query').mockReturnValue(of(new HttpResponse({ body: bookCollection })));
      const additionalBooks = [book];
      const expectedCollection: IBook[] = [...additionalBooks, ...bookCollection];
      jest.spyOn(bookService, 'addBookToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ borrow });
      comp.ngOnInit();

      expect(bookService.query).toHaveBeenCalled();
      expect(bookService.addBookToCollectionIfMissing).toHaveBeenCalledWith(
        bookCollection,
        ...additionalBooks.map(expect.objectContaining),
      );
      expect(comp.booksSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const borrow: IBorrow = { id: 23587 };
      const member: IMember = { id: 17514 };
      borrow.member = member;
      const book: IBook = { id: 32624 };
      borrow.book = book;

      activatedRoute.data = of({ borrow });
      comp.ngOnInit();

      expect(comp.membersSharedCollection).toContainEqual(member);
      expect(comp.booksSharedCollection).toContainEqual(book);
      expect(comp.borrow).toEqual(borrow);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBorrow>>();
      const borrow = { id: 10965 };
      jest.spyOn(borrowFormService, 'getBorrow').mockReturnValue(borrow);
      jest.spyOn(borrowService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ borrow });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: borrow }));
      saveSubject.complete();

      // THEN
      expect(borrowFormService.getBorrow).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(borrowService.update).toHaveBeenCalledWith(expect.objectContaining(borrow));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBorrow>>();
      const borrow = { id: 10965 };
      jest.spyOn(borrowFormService, 'getBorrow').mockReturnValue({ id: null });
      jest.spyOn(borrowService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ borrow: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: borrow }));
      saveSubject.complete();

      // THEN
      expect(borrowFormService.getBorrow).toHaveBeenCalled();
      expect(borrowService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IBorrow>>();
      const borrow = { id: 10965 };
      jest.spyOn(borrowService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ borrow });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(borrowService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareMember', () => {
      it('should forward to memberService', () => {
        const entity = { id: 17514 };
        const entity2 = { id: 30790 };
        jest.spyOn(memberService, 'compareMember');
        comp.compareMember(entity, entity2);
        expect(memberService.compareMember).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareBook', () => {
      it('should forward to bookService', () => {
        const entity = { id: 32624 };
        const entity2 = { id: 17120 };
        jest.spyOn(bookService, 'compareBook');
        comp.compareBook(entity, entity2);
        expect(bookService.compareBook).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
