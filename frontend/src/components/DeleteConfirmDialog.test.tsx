import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DeleteConfirmDialog from './DeleteConfirmDialog';

describe('DeleteConfirmDialog', () => {
  it('should render dialog text when open', () => {
    render(
      <DeleteConfirmDialog open={true} onClose={vi.fn()} onConfirm={vi.fn()} />
    );

    expect(screen.getByText('Delete Schedule')).toBeInTheDocument();
    expect(screen.getByText(/are you sure/i)).toBeInTheDocument();
  });

  it('should not render content when closed', () => {
    render(
      <DeleteConfirmDialog open={false} onClose={vi.fn()} onConfirm={vi.fn()} />
    );

    expect(screen.queryByText('Delete Schedule')).not.toBeInTheDocument();
  });

  it('should call onConfirm when Delete button is clicked', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog open={true} onClose={vi.fn()} onConfirm={onConfirm} />
    );

    await user.click(screen.getByRole('button', { name: /delete/i }));
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('should call onClose when Cancel button is clicked', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog open={true} onClose={onClose} onConfirm={vi.fn()} />
    );

    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
