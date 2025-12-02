import { describe, expect, it } from 'vitest';
import { render, renderWithEmits } from '@/utils/test-utils';
import CommandPaletteItem from '@/components/command-palette/command-palette-item/CommandPaletteItem.vue';

describe('CommandPaletteItem', () => {
  it('should render title properly', () => {
    // given
    const { getByText } = render(CommandPaletteItem, {
      props: {
        title: 'Sample title',
      },
    });

    // expect
    expect(getByText('Sample title')).toBeVisible();
  });

  it('should render subtitle properly', () => {
    // given
    const { getByText } = render(CommandPaletteItem, {
      props: {
        title: 'Sample title',
        subtitle: 'Sample subtitle',
      },
    });

    // expect
    expect(getByText('Sample subtitle')).toBeVisible();
  });

  it('should render icon properly', () => {
    // given
    const { container } = render(CommandPaletteItem, {
      props: {
        title: 'Sample title',
        icon: 'mdi-home',
      },
    });

    // expect
    const icon = container.querySelector('i.mdi-home');
    expect(icon).toBeVisible();
  });

  it('should render label properly', () => {
    // given
    const { getByText } = render(CommandPaletteItem, {
      props: {
        title: 'Sample title',
        label: 'Sample label',
      },
    });

    // expect
    expect(getByText('Sample label')).toBeVisible();
  });

  it('should emit click event on click', async () => {
    // given
    const wrapper = renderWithEmits(CommandPaletteItem, {
      props: {
        title: 'Sample title',
      },
    });

    // when
    await wrapper.find('[role="button"]').trigger('click');

    // then
    expect(wrapper.emitted().click).toBeTruthy();
  });
});
