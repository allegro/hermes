const { generators } = require('../support/generators')
const axios = require('axios').default

const page = {
  url: '/#/groups',
  addGroupButton: '.list-group button',
  alertsList: '#toast-container',
  groupNameInput: 'input#groupName',
  groupList: '.list-group',
  addGroup: (name) => {
    cy.get(page.addGroupButton).click()
    cy.get(page.groupNameInput).click().type(name)
    cy.contains('button', 'Save').click()
  },
}

describe('Groups page', () => {
  const groupName = generators.group.valid()
  it(`allows to add a new group "${groupName}"`, () => {
    cy.visit(page.url)

    page.addGroup(groupName)

    cy.get(page.alertsList).contains('Group has been saved')
    cy.get(page.groupList).get(`a[href="#/groups/${groupName}"]`).should('be.visible').should('contain.text', groupName)
  })

  after(async () => {
    await deleteGroup(groupName)
  })
})

function deleteGroup(groupName) {
  return axios.delete(`${Cypress.config().baseUrl}/groups/${groupName}`)
}

