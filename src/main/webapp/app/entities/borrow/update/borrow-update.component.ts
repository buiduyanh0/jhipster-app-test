import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IMember } from 'app/entities/member/member.model';
import { MemberService } from 'app/entities/member/service/member.service';
import { IBook } from 'app/entities/book/book.model';
import { BookService } from 'app/entities/book/service/book.service';
import { BorrowService } from '../service/borrow.service';
import { IBorrow } from '../borrow.model';
import { BorrowFormGroup, BorrowFormService } from './borrow-form.service';

@Component({
  selector: 'jhi-borrow-update',
  templateUrl: './borrow-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class BorrowUpdateComponent implements OnInit {
  isSaving = false;
  borrow: IBorrow | null = null;

  membersSharedCollection: IMember[] = [];
  booksSharedCollection: IBook[] = [];

  protected borrowService = inject(BorrowService);
  protected borrowFormService = inject(BorrowFormService);
  protected memberService = inject(MemberService);
  protected bookService = inject(BookService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: BorrowFormGroup = this.borrowFormService.createBorrowFormGroup();

  compareMember = (o1: IMember | null, o2: IMember | null): boolean => this.memberService.compareMember(o1, o2);

  compareBook = (o1: IBook | null, o2: IBook | null): boolean => this.bookService.compareBook(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ borrow }) => {
      this.borrow = borrow;
      if (borrow) {
        this.updateForm(borrow);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const borrow = this.borrowFormService.getBorrow(this.editForm);
    if (borrow.id !== null) {
      this.subscribeToSaveResponse(this.borrowService.update(borrow));
    } else {
      this.subscribeToSaveResponse(this.borrowService.create(borrow));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IBorrow>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(borrow: IBorrow): void {
    this.borrow = borrow;
    this.borrowFormService.resetForm(this.editForm, borrow);

    this.membersSharedCollection = this.memberService.addMemberToCollectionIfMissing<IMember>(this.membersSharedCollection, borrow.member);
    this.booksSharedCollection = this.bookService.addBookToCollectionIfMissing<IBook>(this.booksSharedCollection, borrow.book);
  }

  protected loadRelationshipsOptions(): void {
    this.memberService
      .query()
      .pipe(map((res: HttpResponse<IMember[]>) => res.body ?? []))
      .pipe(map((members: IMember[]) => this.memberService.addMemberToCollectionIfMissing<IMember>(members, this.borrow?.member)))
      .subscribe((members: IMember[]) => (this.membersSharedCollection = members));

    this.bookService
      .query()
      .pipe(map((res: HttpResponse<IBook[]>) => res.body ?? []))
      .pipe(map((books: IBook[]) => this.bookService.addBookToCollectionIfMissing<IBook>(books, this.borrow?.book)))
      .subscribe((books: IBook[]) => (this.booksSharedCollection = books));
  }
}
