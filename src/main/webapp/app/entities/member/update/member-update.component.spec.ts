import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { MemberService } from '../service/member.service';
import { IMember } from '../member.model';
import { MemberFormService } from './member-form.service';

import { MemberUpdateComponent } from './member-update.component';

describe('Member Management Update Component', () => {
  let comp: MemberUpdateComponent;
  let fixture: ComponentFixture<MemberUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let memberFormService: MemberFormService;
  let memberService: MemberService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MemberUpdateComponent],
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
      .overrideTemplate(MemberUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(MemberUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    memberFormService = TestBed.inject(MemberFormService);
    memberService = TestBed.inject(MemberService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const member: IMember = { id: 30790 };

      activatedRoute.data = of({ member });
      comp.ngOnInit();

      expect(comp.member).toEqual(member);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IMember>>();
      const member = { id: 17514 };
      jest.spyOn(memberFormService, 'getMember').mockReturnValue(member);
      jest.spyOn(memberService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ member });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: member }));
      saveSubject.complete();

      // THEN
      expect(memberFormService.getMember).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(memberService.update).toHaveBeenCalledWith(expect.objectContaining(member));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IMember>>();
      const member = { id: 17514 };
      jest.spyOn(memberFormService, 'getMember').mockReturnValue({ id: null });
      jest.spyOn(memberService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ member: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: member }));
      saveSubject.complete();

      // THEN
      expect(memberFormService.getMember).toHaveBeenCalled();
      expect(memberService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IMember>>();
      const member = { id: 17514 };
      jest.spyOn(memberService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ member });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(memberService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
