import { Role } from '@/api/role';

export function isAdmin(roles: Role[] | undefined): boolean {
  if (roles) {
    return roles.includes(Role.ADMIN);
  }
  return false;
}

export function isTopicOwnerOrAdmin(roles: Role[] | undefined): boolean {
  if (roles) {
    return roles.includes(Role.ADMIN) || roles.includes(Role.TOPIC_OWNER);
  }
  return false;
}

export function isSubscriptionOwnerOrAdmin(roles: Role[] | undefined): boolean {
  if (roles) {
    return (
      roles.includes(Role.ADMIN) || roles.includes(Role.SUBSCRIPTION_OWNER)
    );
  }
  return false;
}
