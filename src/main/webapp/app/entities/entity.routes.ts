import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'authority',
    data: { pageTitle: 'Authorities' },
    loadChildren: () => import('./admin/authority/authority.routes'),
  },
  {
    path: 'book',
    data: { pageTitle: 'Books' },
    loadChildren: () => import('./book/book.routes'),
  },
  {
    path: 'member',
    data: { pageTitle: 'Members' },
    loadChildren: () => import('./member/member.routes'),
  },
  {
    path: 'borrow',
    data: { pageTitle: 'Borrows' },
    loadChildren: () => import('./borrow/borrow.routes'),
  },
  /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
];

export default routes;
