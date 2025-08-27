import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IBook } from '../book.model';
import { BookService } from '../service/book.service';
import { BookFormGroup, BookFormService } from './book-form.service';

@Component({
  selector: 'jhi-book-update',
  templateUrl: './book-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class BookUpdateComponent implements OnInit {
  isSaving = false;
  book: IBook | null = null;

  protected bookService = inject(BookService);
  protected bookFormService = inject(BookFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: BookFormGroup = this.bookFormService.createBookFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ book }) => {
      this.book = book;
      if (book) {
        this.updateForm(book);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const book = this.bookFormService.getBook(this.editForm);
    if (book.id !== null) {
      this.subscribeToSaveResponse(this.bookService.update(book));
    } else {
      this.subscribeToSaveResponse(this.bookService.create(book));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IBook>>): void {
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

  protected updateForm(book: IBook): void {
    this.book = book;
    this.bookFormService.resetForm(this.editForm, book);
  }
}
