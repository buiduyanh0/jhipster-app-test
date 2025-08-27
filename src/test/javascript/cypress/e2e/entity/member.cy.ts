import {
  entityConfirmDeleteButtonSelector,
  entityCreateButtonSelector,
  entityCreateCancelButtonSelector,
  entityCreateSaveButtonSelector,
  entityDeleteButtonSelector,
  entityDetailsBackButtonSelector,
  entityDetailsButtonSelector,
  entityEditButtonSelector,
  entityTableSelector,
} from '../../support/entity';

describe('Member e2e test', () => {
  const memberPageUrl = '/member';
  const memberPageUrlPattern = new RegExp('/member(\\?.*)?$');
  const username = Cypress.env('E2E_USERNAME') ?? 'user';
  const password = Cypress.env('E2E_PASSWORD') ?? 'user';
  const memberSample = { name: 'whoa', email: 'ThienLam.Trinh@hotmail.com', joinDate: '2025-08-21T14:28:14.272Z' };

  let member;

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/members+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/members').as('postEntityRequest');
    cy.intercept('DELETE', '/api/members/*').as('deleteEntityRequest');
  });

  afterEach(() => {
    if (member) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/members/${member.id}`,
      }).then(() => {
        member = undefined;
      });
    }
  });

  it('Members menu should load Members page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('member');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('Member').should('exist');
    cy.url().should('match', memberPageUrlPattern);
  });

  describe('Member page', () => {
    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(memberPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create Member page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.url().should('match', new RegExp('/member/new$'));
        cy.getEntityCreateUpdateHeading('Member');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', memberPageUrlPattern);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/members',
          body: memberSample,
        }).then(({ body }) => {
          member = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/members+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              body: [member],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(memberPageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details Member page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('member');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', memberPageUrlPattern);
      });

      it('edit button click should load edit Member page and go back', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Member');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', memberPageUrlPattern);
      });

      it('edit button click should load edit Member page and save', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Member');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', memberPageUrlPattern);
      });

      it('last delete button click should delete instance of Member', () => {
        cy.get(entityDeleteButtonSelector).last().click();
        cy.getEntityDeleteDialogHeading('member').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.url().should('match', memberPageUrlPattern);

        member = undefined;
      });
    });
  });

  describe('new Member page', () => {
    beforeEach(() => {
      cy.visit(`${memberPageUrl}`);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('Member');
    });

    it('should create an instance of Member', () => {
      cy.get(`[data-cy="name"]`).type('pish disk amongst');
      cy.get(`[data-cy="name"]`).should('have.value', 'pish disk amongst');

      cy.get(`[data-cy="email"]`).type('ThanhDoanh75@gmail.com');
      cy.get(`[data-cy="email"]`).should('have.value', 'ThanhDoanh75@gmail.com');

      cy.get(`[data-cy="joinDate"]`).type('2025-08-21T11:56');
      cy.get(`[data-cy="joinDate"]`).blur();
      cy.get(`[data-cy="joinDate"]`).should('have.value', '2025-08-21T11:56');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        member = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.url().should('match', memberPageUrlPattern);
    });
  });
});
